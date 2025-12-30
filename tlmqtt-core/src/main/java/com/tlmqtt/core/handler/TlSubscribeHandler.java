package com.tlmqtt.core.handler;

import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlSubscribeHandler extends AbstractTlHandler<TlMqttSubscribeReq> {


    protected final ForwardMessageService forwardMessageService;

    public TlSubscribeHandler(ForwardMessageService forwardMessageService, AuthorizationManager authorizationManager,
        ShareSubscribeService shareSubscribeService, SessionService sessionService,
        RetainService retainService, PublishService publishService,
        SubscriptionService subscriptionService) {
        this.forwardMessageService = forwardMessageService;
        super.setAuthorizationManager(authorizationManager);
        super.setSessionService(sessionService);
        super.setShareSubscribeService(shareSubscribeService);
        super.setPublishService(publishService);
        super.setRetainService(retainService);
        super.setSubscriptionService(subscriptionService);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttSubscribeReq req, TlMqttSession session) {
        int messageId = req.getVariableHead().getMessageId();
        List<TlTopic> topics = req.getPayload().getTopics();

        String clientId = session.getClientId();
        List<TlTopic> authorizedTopics = new ArrayList<>();
        int[] reasonCodes = new int[topics.size()];

        // 1. 权限校验与原因码准备
        for (int i = 0; i < topics.size(); i++) {
            TlTopic topic = topics.get(i);
            if (authorizationManager.checkSubscribePermission(session, topic.getName())) {
                reasonCodes[i] = topic.getQos();
                authorizedTopics.add(topic);
                log.debug("客户端【{}】订阅了主题【{}】-OQS是【{}】",clientId,topic.getName(),topic.getQos());
            } else {
                // MQTT 5.0 0x87 (Not Authorized)
                log.debug("客户端【{}】无权鼎业主题【{}】-OQS是【{}】",clientId,topic.getName(),topic.getQos());
                reasonCodes[i] = MqttErrorCode.UNAUTHORIZED.byteValue();
            }
        }

        // 2. 发送 SUBACK
        TlMqttSubAck subAck = TlMqttSubAck.build(reasonCodes, messageId, null, null);
        ctx.writeAndFlush(subAck).addListener(future -> {
            if (future.isSuccess() && !authorizedTopics.isEmpty()) {
                // 3. 执行订阅持久化与保留消息分发
                executeSubscriptionLogic(session, authorizedTopics, req, ctx);
            }
        });
    }

    private void executeSubscriptionLogic(TlMqttSession session, List<TlTopic> topics, TlMqttSubscribeReq req, ChannelHandlerContext ctx) {
        String clientId = session.getClientId();
        Integer subId = req.getVariableHead().getSubscriptionIdentifier();

        // 更新 Session 内存中的主题列表
        Set<String> newTopicNames = topics.stream().map(TlTopic::getName).collect(Collectors.toSet());
        session.getTopics().addAll(newTopicNames);

        sessionService.save(session)
            .thenMany(Flux.fromIterable(topics))
            .flatMap(topic -> {
                TlSubClient client = buildSubClient(session, topic, subId);

                // 根据是否共享订阅调用不同服务
                Mono<Boolean> subMono = client.getIsShared()
                    ? shareSubscribeService.subscribeShare(client)
                    : subscriptionService.subscribe(client);

                return subMono.thenMany(handleRetainMessages(session, topic, client, ctx));
            })
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Subscription process error for client [{}]", clientId, e))
            .subscribe();
    }

    private Flux<Void> handleRetainMessages(TlMqttSession session, TlTopic topic, TlSubClient client, ChannelHandlerContext ctx) {
        // MQTT 5.0 Retain Handling:
        // 2 = 不发送保留消息
        if (topic.getRetainHandling() != null && topic.getRetainHandling() == 2) {
            return Flux.empty();
        }

        // TODO: 如果 Retain Handling == 1，需判断是否是“重复订阅”，只有新订阅才发。目前简化处理。

        return retainService.find(topic.getName())
            .filter(publishReq -> !isExpired(publishReq))
            .flatMap(publishReq -> {
                // 计算 QoS 降级
                int subQos = topic.getQos();
                int pubQos = publishReq.getFixedHead().getQos().value();
                MqttQoS realQos = MqttQoS.valueOf(Math.min(subQos, pubQos));

                // 使用 ForwardMessageService 的 build 方法，它会自动从 Session 分配 MessageId
                TlMqttPublishReq targetMessage = forwardMessageService.buildTargetReq(publishReq, realQos, session, client);

                // 如果是 QoS 1/2，需要持久化并处理流量窗口
                if (realQos != MqttQoS.AT_MOST_ONCE) {
                    return publishService.save(session.getClientId(), targetMessage.getVariableHead().getMessageId(), targetMessage)
                        .doOnSuccess(v -> {
                            // 占用 In-Flight 窗口并发送
                            session.getInFlightCount().incrementAndGet();
                            ctx.writeAndFlush(targetMessage);
                        }).then();
                } else {
                    ctx.writeAndFlush(targetMessage);
                    return Mono.empty();
                }
            });
    }

    private TlSubClient buildSubClient(TlMqttSession session, TlTopic topic, Integer subId) {
        return TlSubClient.builder()
            .clientId(session.getClientId())
            .topic(topic.getName())
            .qos(topic.getQos())
            .mqttVersion(session.getMqttVersion())
            .subscriptionIdentifier(subId)
            .isShared(topic.isShare())
            .group(topic.getGroup())
            .noLocal(topic.getNoLocal())
            .retainAsPublished(topic.getRetainAsPublished())
            .build();
    }

    private boolean isExpired(TlMqttPublishReq req) {
        if (req.getVariableHead().getMessageExpiryInterval() == null) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        return (req.getAcceptTime() + req.getVariableHead().getMessageExpiryInterval()) < now;
    }

}