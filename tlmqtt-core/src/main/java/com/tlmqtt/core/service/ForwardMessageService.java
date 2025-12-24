package com.tlmqtt.core.service;

import cn.hutool.core.util.StrUtil;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.core.task.TlRetryTask;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ChannelManager channelManager;
    private final RetryManager retryManager;

    public ForwardMessageService(AliasService aliasService, ShareSubscribeService shareSubscribeService,
        IShareSubscribeClientChoose shareSubscribeClientChoose, SubscriptionService subscriptionService,
        SessionService sessionService, PublishService publishService,
        ChannelManager channelManager, RetryManager retryManager) {
        this.aliasService = aliasService;
        this.shareSubscribeService = shareSubscribeService;
        this.shareSubscribeClientChoose = shareSubscribeClientChoose;
        this.subscriptionService = subscriptionService;
        this.sessionService = sessionService;
        this.publishService = publishService;
        this.channelManager = channelManager;
        this.retryManager = retryManager;
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
            if (groups == null || groups.isEmpty()) return Flux.empty();
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
                if (session.getMqttVersion() == MqttVersion.MQTT_5 && session.getMaximumPacketSize() != null) {
                    if (calculateSize(targetReq) > session.getMaximumPacketSize()) {
                        log.warn("Packet too large for [{}], drop.", targetClientId);
                        return Mono.empty();
                    }
                }

                // 5. 流量控制（In-Flight 检查）
                return processWithTrafficControl(session, targetReq);
            });
    }

    /**
     * 流量控制核心：QoS 0 直接发，QoS 1/2 检查窗口
     */
    private Mono<Void> processWithTrafficControl(TlMqttSession session, TlMqttPublishReq req) {
        MqttQoS qos = req.getFixedHead().getQos();
        String clientId = session.getClientId();

        if (qos == MqttQoS.AT_MOST_ONCE) {
            sendToNetty(req, clientId);
            return Mono.empty();
        }

        // 检查 In-Flight 窗口
        int maxInFlight = session.getReceiveMaximum() != null ? session.getReceiveMaximum() : 65535;
        if (session.getInFlightCount().get() >= maxInFlight) {
            log.debug("Client [{}] In-Flight full, queuing message", clientId);
            session.getMessageQueue().offer(req);
            return Mono.empty();
        }

        // 占用窗口并存储待确认消息
        session.getInFlightCount().incrementAndGet();
        return publishService.save(clientId, req.getVariableHead().getMessageId(), req)
            .doOnSuccess(v -> sendToNetty(req, clientId))
            .then();
    }

    /**
     * 构建发送报文副本
     */
    public TlMqttPublishReq buildTargetReq(TlMqttPublishReq req, MqttQoS qos, TlMqttSession session, TlSubClient sub) {
        TlMqttPublishVariableHead oldVHead = req.getVariableHead();

        // 分配 16 位消息 ID
        Long messageId = (qos != MqttQoS.AT_MOST_ONCE) ? (long) session.getMessageIdManager().getNextId() : 0L;

        TlMqttPublishVariableHead newVHead = TlMqttPublishVariableHead.builder()
            .topic(oldVHead.getTopic())
            .messageId(messageId)
            .payloadFormatIndicator(oldVHead.getPayloadFormatIndicator())
            .messageExpiryInterval(oldVHead.getMessageExpiryInterval())
            .responseTopic(oldVHead.getResponseTopic())
            .correlationData(oldVHead.getCorrelationData())
            .contentType(oldVHead.getContentType())
            .subscriptionIdentifier(sub.getSubscriptionIdentifier())
            .build();

        boolean retain = req.getFixedHead().isRetain() && (sub.getRetainAsPublished() != null && sub.getRetainAsPublished());

        TlMqttFixedHead newFixedHead = TlMqttFixedHead.builder()
            .messageType(MqttMessageType.PUBLISH)
            .qos(qos)
            .retain(retain)
            .dup(false)
            .build();

        TlMqttPublishReq targetReq = TlMqttPublishReq.build(newFixedHead, newVHead, req.getPayload(), session.getMqttVersion());
        targetReq.setAcceptTime(req.getAcceptTime());
        return targetReq;
    }

    /**
     * 确认回调 (由 PubAckHandler/PubCompHandler 调用)
     */
    public void handleAck(String clientId, int messageId) {
        sessionService.find(clientId).subscribe(session -> {
            // 1. 释放 ID 和 窗口
            session.getMessageIdManager().releaseId(messageId);
            session.getInFlightCount().decrementAndGet();

            // 2. 触发队列中的下一条消息
            TlMqttPublishReq next = session.getMessageQueue().poll();
            if (next != null) {
                this.processWithTrafficControl(session, next).subscribe();
            }
        });
    }

    private void sendToNetty(TlMqttPublishReq req, String clientId) {
        Channel channel = channelManager.getChannel(clientId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(req).addListener(f -> {
                if (f.isSuccess() && req.getFixedHead().getQos().value() > 0) {
                    // 注册重试
                    retryManager.schedulePublishRetry(req.getVariableHead().getMessageId(),
                        new TlRetryTask(req.getVariableHead().getMessageId(), req, channel));
                }
            });
        }
    }

    private String handleTopicAlias(String clientId, Integer alias, String topic) {

        if (StrUtil.isNotEmpty(topic)) {
            aliasService.put(clientId, alias, topic);
            return topic;
        } else {
            String cached = aliasService.get(clientId, alias);
            if (cached == null) throw new TlProtocolErrorException(MqttMessageType.PUBLISH);
            return cached;
        }
    }

    private int calculateSize(TlMqttPublishReq req) {
        // 简化的长度计算逻辑，实际需根据编码后的字节数组长度判定
        return req.getFixedHead().getLength();
    }
}
