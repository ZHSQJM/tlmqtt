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
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
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
    private final PubrelService pubrelService;
    private final TlChannelService channelService;
    private final TlSchedulerTaskService schedulerTaskService;
    /**重试间隔秒*/
    private final int retryInterval;
    /**最大重试次数*/
    private final int maxRetries;
    public ForwardMessageService(AliasService aliasService, ShareSubscribeService shareSubscribeService,
        IShareSubscribeClientChoose shareSubscribeClientChoose, SubscriptionService subscriptionService,
        SessionService sessionService, PublishService publishService,
        TlChannelService channelService,TlSchedulerTaskService schedulerTaskService,PubrelService pubrelService,int retryInterval, int maxRetries ) {
        this.aliasService = aliasService;
        this.shareSubscribeService = shareSubscribeService;
        this.shareSubscribeClientChoose = shareSubscribeClientChoose;
        this.subscriptionService = subscriptionService;
        this.sessionService = sessionService;
        this.publishService = publishService;
        this.pubrelService = pubrelService;
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
        // 引用计数 +1，防止在 Flux 遍历期间被释放
        if (req.getPayload() != null && req.getPayload().getContent() != null) {
            ReferenceCountUtil.retain(req.getPayload().getContent());
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
            .doFinally(signal -> {
                // 最终释放 Payload
                if (req.getPayload() != null && req.getPayload().getContent() != null) {
                    ReferenceCountUtil.safeRelease(req.getPayload().getContent());
                }
            })
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
                        log.warn("Packet too large for [{}], drop.", targetClientId);
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
            log.debug("Client [{}] In-Flight full, queuing message", clientId);
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
            log.warn("Unknown message type for retry: {}", message.getClass().getName());
            return Mono.empty();
        }

        // 2. 生成唯一的调度 Key (例如: clientId:PUBLISH:1001)
        String retryKey = buildScheduleKey(clientId, type, messageId);

        // 3. 检查重试次数限制
        if (count > maxRetries) {
            log.warn("Task [{}] reached max retries ({}), dropping message.", retryKey, maxRetries);
            // 这里可以根据业务需求增加持久化清理逻辑
            return Mono.empty();
        }

        // 4. 执行发送逻辑
        return Mono.defer(() -> {
            // 只有 PUBLISH 报文在重发(count > 1)时需要设置 DUP 标志
            if (count > 1 && message instanceof TlMqttPublishReq) {
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
                // 注意：ByteBuf 的引用计数在重试场景下非常危险
                // 如果 msg 包含 ByteBuf，Netty 发送后会自动释放。
                // 建议重试时使用 retain() 或者发送不释放的副本
                if (msg instanceof TlMqttPublishReq ) {
                    // 增加引用计数，确保 Netty 发送完一次后，内存不被彻底回收，供下一次重试使用
                    TlMqttPublishReq publishReq = (TlMqttPublishReq) msg;
                    ReferenceCountUtil.retain(publishReq.getPayload().getContent());
                }

                channel.writeAndFlush(msg).addListener(f -> {
                    if (f.isSuccess()) {
                        sink.success();
                    } else {
                        sink.error(f.cause());
                    }
                });
            } else {
                sink.error(new RuntimeException("Channel inactive for client: " + clientId));
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
            .doOnSuccess(v -> log.debug("Stopped retry for client: [{}], type: [{}], id: [{}]", clientId, type, messageId))
            .subscribe();

        // 2. 根据业务类型执行后续清理
        if (Constant.PUBLISH.equals(type)) {
            // QoS 1 流程：清理持久化消息 -> 释放窗口 -> 触发队列
            publishService.clear(clientId, (long) messageId)
                .then(processNextInQueue(clientId, messageId))
                .subscribe();
        } else if (Constant.PUBREL.equals(type)) {
            // QoS 2 流程：清理 PUBREL 持久化 -> 释放窗口 -> 触发队列
            // 注意：此时 PUBLISH 已经在收到 PUBREC 时被清理过了
            pubrelService.clear(clientId, (long) messageId)
                .then(processNextInQueue(clientId, messageId))
                .subscribe();
        }
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
                    log.debug("Polling next message from queue for client: [{}]", clientId);
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
        // 简化的长度计算逻辑，实际需根据编码后的字节数组长度判定
        return req.getFixedHead().getLength();
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
