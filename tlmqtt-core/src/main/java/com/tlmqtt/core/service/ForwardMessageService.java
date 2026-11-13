package com.tlmqtt.core.service;

import cn.hutool.core.util.StrUtil;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.alias.AliasService;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class ForwardMessageService {

    private final AliasService aliasService;
    private final ShareSubscribeService shareSubscribeService;
    private final IShareSubscribeClientChoose shareSubscribeClientChoose;
    private final SubscriptionService subscriptionService;
    private final SessionService sessionService;
    private final PublishService publishService;
    private final TlChannelService channelService;
    private final TlSchedulerTaskService schedulerTaskService;
    /**重试间隔秒*/
    private final int retryInterval;
    /**最大重试次数*/
    private final int maxRetries;
    public ForwardMessageService(AliasService aliasService, ShareSubscribeService shareSubscribeService,
        IShareSubscribeClientChoose shareSubscribeClientChoose, SubscriptionService subscriptionService,
        SessionService sessionService, PublishService publishService,
        TlChannelService channelService,TlSchedulerTaskService schedulerTaskService,int retryInterval, int maxRetries ) {
        this.aliasService = aliasService;
        this.shareSubscribeService = shareSubscribeService;
        this.shareSubscribeClientChoose = shareSubscribeClientChoose;
        this.subscriptionService = subscriptionService;
        this.sessionService = sessionService;
        this.publishService = publishService;
        this.channelService = channelService;
        this.schedulerTaskService = schedulerTaskService;
        this.retryInterval  = retryInterval;
        this.maxRetries = maxRetries;

    }

    /**
     * 消息发布主入口
     */
    public Mono<Void> publish(TlMqttPublishReq req, String publisherId, MqttVersion version) {
        TlMqttPublishVariableHead variableHead = req.getVariableHead();
        String topic = variableHead.getTopic();

        // 1. MQTT 5.0 主题别名解析
        if (version == MqttVersion.MQTT_5 && variableHead.getTopicAlias() != null) {
            topic = handleTopicAlias(publisherId, variableHead.getTopicAlias(), topic);
            variableHead.setTopic(topic);
        }
        // 2. 汇聚订阅者（普通订阅 + 共享订阅）
        String finalTopic = topic;
        Flux<TlSubClient> normalSubs = Flux.defer(() ->subscriptionService.find(finalTopic));

        String finalTopic1 = topic;
        Flux<TlSubClient> shareSubs = Flux.defer(() -> {
            Map<String, List<TlSubClient>> groups = shareSubscribeService.getGroupMember(finalTopic1);
            if (groups == null || groups.isEmpty()) {
                return Flux.empty();
            }
            return Flux.fromIterable(groups.values())
                .map(list -> shareSubscribeClientChoose.choose(list, publisherId));
        });

        // 3. 并行执行转发逻辑
        return Flux.merge(normalSubs, shareSubs)
            // 防止重复订阅导致重复发送
            .distinct(TlSubClient::getClientId)
            .flatMap(subClient -> doForward(req, subClient, publisherId))
            .then();
    }

    /**
     * 执行具体转发
     */
    private Mono<Void> doForward(TlMqttPublishReq originalReq, TlSubClient subClient, String publisherId) {
        String targetClientId = subClient.getClientId();

        return sessionService.find(targetClientId)
            .flatMap(session -> {
                // 1. NoLocal 检查
                if (subClient.getNoLocal() != null && subClient.getNoLocal() && targetClientId.equals(publisherId)) {
                    return Mono.empty();
                }

                // 2. QoS 降级处理
                MqttQoS realQos = MqttQoS.valueOf(Math.min(originalReq.getFixedHead().getQos().value(), subClient.getQos()));

                // 3. 构建适配目标客户端的报文 (包含 ID 分配)
                TlMqttPublishReq targetReq = buildTargetReq(originalReq, realQos, session, subClient);

                // 4. MQTT 5.0 报文长度检查
                if (session.isVersion5() && session.getMaximumPacketSize() != null) {
                    if (calculateSize(targetReq) > session.getMaximumPacketSize()) {
                        log.warn("【TLMQTT】Packet too large for [{}], drop.", targetClientId);
                        return Mono.empty();
                    }
                }
                if (realQos == MqttQoS.AT_MOST_ONCE) {
                    // QoS 0 绕过所有 In-Flight 和重试逻辑，直接发送，减少 Reactor 链条长度
                    return doSend(targetReq, targetClientId);
                } else {
                    // QoS 1/2 进入复杂的流量控制和重试逻辑       // 5. 流量控制（In-Flight 检查）
                    return processWithTrafficControl(session, targetReq);
                }

            })
            // 隔离单个订阅者的失败，避免一个离线/异常的订阅者拖垮整条转发链
            .onErrorResume(e -> {
                log.warn("【TLMQTT】Forward to client [{}] failed, skip: {}", targetClientId, e.getMessage());
                return Mono.empty();
            });
    }

    /**
     * 流量控制核心：QoS 0 直接发，QoS 1/2 检查窗口
     */
    private Mono<Void> processWithTrafficControl(TlMqttSession session, TlMqttPublishReq req) {
        String clientId = session.getClientId();

        // QoS 1/2 流量控制
        int maxInFlight = session.getReceiveMaximum() != null ? session.getReceiveMaximum() : 65535;
        if (session.getInFlightCount().get() >= maxInFlight) {
            log.debug("【TLMQTT】Client [{}] In-Flight full, queuing message", clientId);
            session.getMessageQueue().offer(req);
            return Mono.empty();
        }

        // 占用窗口并启动重试流水线
        session.getInFlightCount().incrementAndGet();
        long msgId = req.getVariableHead().getMessageId();
        return publishService.save(clientId, msgId, req)
            .then(scheduleWithRetry(Constant.PUBLISH, clientId, req, 1));
    }

    /**
     * 构建发送报文副本
     */
    public TlMqttPublishReq buildTargetReq(TlMqttPublishReq req, MqttQoS qos, TlMqttSession session, TlSubClient sub) {
        TlMqttPublishVariableHead oldHead = req.getVariableHead();

        // 分配 16 位消息 ID
        Long messageId = (qos != MqttQoS.AT_MOST_ONCE) ? (long) session.getMessageIdManager().getNextId() : 0L;

        TlMqttPublishVariableHead newHead = TlMqttPublishVariableHead.builder()
            .topic(oldHead.getTopic())
            .messageId(messageId)
            .payloadFormatIndicator(oldHead.getPayloadFormatIndicator())
            .messageExpiryInterval(oldHead.getMessageExpiryInterval())
            .responseTopic(oldHead.getResponseTopic())
            .correlationData(oldHead.getCorrelationData())
            .contentType(oldHead.getContentType())
            .subscriptionIdentifier(sub.getSubscriptionIdentifier())
            .build();

        boolean retain = req.getFixedHead().isRetain() && (sub.getRetainAsPublished() != null && sub.getRetainAsPublished());

        TlMqttFixedHead newFixedHead = TlMqttFixedHead.builder()
            .messageType(MqttMessageType.PUBLISH)
            .qos(qos)
            .retain(retain)
            .dup(false)
            .build();

        TlMqttPublishReq targetReq = TlMqttPublishReq.build(newFixedHead, newHead, req.getPayload(), session.getMqttVersion());
        targetReq.setAcceptTime(req.getAcceptTime());
        return targetReq;
    }


    /**
     * 2. 优化：合并 PUBLISH 和 PUBREL 的重试逻辑
     * 使用泛型或 Object 抽象，减少代码重复
     */
    public Mono<Void> scheduleWithRetry(String type, String clientId, AbstractTlMessage message, int count) {
        if (null == message) {
            return Mono.empty();
        }

        // 1. 统一提取消息 ID
        long messageId = getMessageId(message);
        if (messageId == -1) {
            log.warn("【TLMQTT】Unknown message type for retry: {}", message.getClass().getName());
            return Mono.empty();
        }

        // 2. 生成唯一的调度 Key (例如: clientId:PUBLISH:1001)
        String retryKey = buildScheduleKey(clientId, type, messageId);

        // 3. 检查重试次数限制：超限后必须回收资源，否则 messageId / In-Flight 窗口会泄漏，最终耗尽 65535 个 ID
        if (count > maxRetries) {
            log.warn("【TLMQTT】Task [{}] reached max retries ({}), dropping and releasing resources.", retryKey, maxRetries);
            return cleanup(type, clientId, (int) messageId);
        }

        // 4. 执行发送逻辑
        return Mono.defer(() -> {
            // 重发(count > 1)时设置 DUP 标志，PUBLISH 与 PUBREL 均需设置
            if (count > 1) {
                message.getFixedHead().setDup(true);
            }
            // 这里会执行所有类型的消息发送，包括 TlMqttPublishReq 和 TlMqttPubRelReq
            return doSend(message, clientId);
        }).then(
            // 5. 关键修正：递归调用时传入 type 而不是上一次生成的 retryKey
            schedulerTaskService.schedule(
                retryKey,
                Mono.defer(() -> scheduleWithRetry(type, clientId, message, count + 1)),
                retryInterval,
                TimeUnit.SECONDS
            )
        );
    }

    /**
     * 3. 优化：QoS 2 流程停止方法封装
     */
    public Mono<Void> cancel(String clientId, String type,long messageId) {
        String retryKey =  buildScheduleKey(clientId,type,messageId);
        return schedulerTaskService.cancel(retryKey);
    }

    /**
     * 4. 优化：Netty 发送动作增加安全性检查
     */
    private Mono<Void> doSend(Object msg, String clientId) {
        return Mono.create(sink -> {
            Channel channel = channelService.getChannel(clientId);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(msg).addListener(f -> {
                    if (f.isSuccess()) {
                        sink.success();
                    } else {
                        sink.error(f.cause());
                    }
                });
            } else {
                // 订阅者离线：不视为错误，交由重试机制兜底（QoS 1/2），QoS 0 直接丢弃
                log.debug("【TLMQTT】Channel inactive for client: [{}], skip send", clientId);
                sink.success();
            }
        });
    }
    /**
     * 统一确认回调入口
     * @param clientId 客户端ID
     * @param type 类型 (Constant.PUBLISH 或 Constant.PUBREL)
     * @param messageId 消息ID
     */
    public void handleAck(String clientId, String type, int messageId) {
        // 1. 无论什么类型，先停止重试定时器
        cancel(clientId, type, messageId)
            .doOnSuccess(v -> log.debug("【TLMQTT】Stopped retry for client: [{}], type: [{}], id: [{}]", clientId, type, messageId))
            .subscribe();

        // 2. 根据业务类型执行后续清理（清理持久化 -> 释放窗口 -> 触发队列）
        cleanup(type, clientId, messageId).subscribe();
    }

    /**
     * 统一的资源回收逻辑：清理持久化消息、释放 messageId、减小 In-Flight 窗口、触发队列中的下一条消息
     * 用于 ACK 确认与重试超限两种场景，避免 messageId / In-Flight 窗口泄漏
     */
    private Mono<Void> cleanup(String type, String clientId, int messageId) {
        if (Constant.PUBLISH.equals(type)) {
            // QoS 1 流程
            return publishService.clear(clientId, (long) messageId)
                .then(processNextInQueue(clientId, messageId));
        } else if (Constant.PUBREL.equals(type)) {
            // QoS 2 流程：此时 PUBLISH 已经在收到 PUBREC 时被清理过了
            return publishService.clearPubrel(clientId, (long) messageId)
                .then(processNextInQueue(clientId, messageId));
        }
        return Mono.empty();
    }

    /**
     * 通用的窗口回收与队列触发逻辑
     */
    private Mono<Void> processNextInQueue(String clientId, int messageId) {
        return sessionService.find(clientId)
            .flatMap(session -> {
                // 1. 释放 ID 资源
                session.getMessageIdManager().releaseId(messageId);
                // 2. 减小 In-Flight 计数并尝试处理积压队列
                if (session.getInFlightCount().get() > 0) {
                    session.getInFlightCount().decrementAndGet();
                }

                // 3. 触发队列中的下一条消息
                TlMqttPublishReq next = session.getMessageQueue().poll();
                if (next != null) {
                    log.debug("【TLMQTT】Polling next message from queue for client: [{}]", clientId);
                    // 递归回 processWithTrafficControl 逻辑
                    return this.processWithTrafficControl(session, next);
                }
                return Mono.empty();
            })
            .then();
    }
    private String handleTopicAlias(String clientId, Integer alias, String topic) {

        if (StrUtil.isNotEmpty(topic)) {
            aliasService.put(clientId, alias, topic);
            return topic;
        } else {
            String cached = aliasService.get(clientId, alias);
            if (cached == null) {
                throw new TlProtocolErrorException(MqttMessageType.PUBLISH,MqttMessageType.DISCONNECT);
            }
            return cached;
        }
    }

    private int calculateSize(TlMqttPublishReq req) {
        // fixedHead.length 已由 TlMqttPublishReq.build() 精确计算为剩余长度（remainingLength），
        // 这里再补上固定头（1 字节）与变长长度字段的字节数，得到整包大小，供 MaximumPacketSize 校验。
        int remainingLength = req.getFixedHead().getLength();
        return 1 + variableByteIntegerLength(remainingLength) + remainingLength;
    }

    /**
     * 计算变长字节整数（Variable Byte Integer）占用的字节数
     */
    private int variableByteIntegerLength(int value) {
        int length = 1;
        while ((value /= 128) > 0) {
            length++;
        }
        return length;
    }



    public String buildScheduleKey(String clientId, String key, long msgId) {
        return clientId + ":" + key + ":" + msgId;
    }

    /**
     * 辅助方法：从不同类型的消息中提取 ID
     */
    private long getMessageId(AbstractTlMessage message) {
        if (message instanceof TlMqttPublishReq) {
            return ((TlMqttPublishReq) message).getVariableHead().getMessageId();
        } else if (message instanceof TlMqttPubRelReq) {
            return ((TlMqttPubRelReq) message).getVariableHead().getMessageId();
        }
        return -1;
    }
}
