package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttDataSource;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.common.model.response.TlMqttPubAck;
import com.tlmqtt.common.rule.EventContext;
import com.tlmqtt.common.rule.RuleEngineDispatcher;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlExceptionHandler extends ChannelInboundHandlerAdapter {

    private final PublishService publishService;
    private final TlChannelService channelService;
    private final SessionService sessionService;
    private final MqttConfiguration mqttConfiguration;
    private final SubscriptionService subscriptionService;
    private final ForwardMessageService forwardMessageService;
    private final TlSchedulerTaskService schedulerTaskService;
    private final RuleEngineDispatcher ruleEngineDispatcher;
    private final int EXPIRY_INTERVAL;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        TlMqttSession session = (TlMqttSession) channel.attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        if (session == null) {
            ctx.close();
            return;
        }

        String clientId = session.getClientId();

        EventContext eventContext = EventContext.builder().dataSource(MqttDataSource.CLIENT_DISCONNECTED).clientId(clientId)
            .ip("127.0.0.1").timestamp(System.currentTimeMillis()).build();
        ruleEngineDispatcher.dispatch(eventContext);
        // 1. 判定是否为“冲突剔除”。如果是，不触发遗嘱和清理逻辑
        if (channelService.getChannel(clientId) != channel) {
            log.debug("【TLMQTT】Channel for client: [{}] has been replaced, skip cleanup", clientId);
            return;
        }

        // 2. 串行处理：遗嘱 -> 会话清理 -> 关闭通道
        handleWillMessage(session)
            .then(handleSessionCleanup(session))
            .doFinally(signalType -> {
                channelService.remove(clientId);
                ctx.close();
            })
            .subscribe(
                null,
                e -> log.error("【TLMQTT】Error during channel inactive cleanup for [{}]: {}", clientId, e.getMessage())
            );
    }

    private Mono<Void> handleSessionCleanup(TlMqttSession session) {
        String clientId = session.getClientId();
        String expiryKey = clientId + Constant.MQTT_SESSION;

        // 1. 如果是 CleanSession (3.1.1) 或 ExpiryInterval == 0 (5.0) -> 立即清理
        if (shouldClearImmediately(session)) {

            log.debug("【TLMQTT】Immediately clearing session for client [{}]", clientId);
            return sessionService.clearAll(clientId).then();
        }

        // 2. 否则，执行延迟清理逻辑
        int expiryInterval = getExpiryInterval(session);

        // 构造完整的清理任务（包含消息、订阅、Session状态）
        Mono<Void> expiryTask = sessionService.clearAll(clientId)
            .doOnSuccess(v -> log.debug("【TLMQTT】 clean client【{}】session task over", clientId))
            .then();
        log.debug("【TLMQTT】clean client[{}] taks after【{}】second execute", clientId, expiryInterval);
        return schedulerTaskService.schedule(
            expiryKey,
            expiryTask,
            expiryInterval,
            TimeUnit.SECONDS
        );
    }
    /**
     * 判断是否需要立即清理
     */
    private boolean shouldClearImmediately(TlMqttSession session) {
        if (session.isVersion5()) {
            return session.getSessionExpiryInterval() <= 0;
        } else {
            return session.isCleanSession();
        }
    }

    /**
     * 获取过期时间间隔
     */
    private int getExpiryInterval(TlMqttSession session) {
        if (session.isVersion5()) {
            return session.getSessionExpiryInterval();
        }
        // 对于 MQTT 3.1.1 cleanSession=false，建议设置一个全局默认过期时间（如 24 小时）
        // 而不是无限保留，防止内存溢出。
        return EXPIRY_INTERVAL;
    }

    private Mono<Void> handleWillMessage(TlMqttSession session) {
        String clientId = session.getClientId();

        // 如果是正常断开 (发送了 DISCONNECT 报文)，则取消遗嘱
        if (isNormalDisconnect(session.getCtx().channel())) {
            log.debug("【TLMQTT】Client: [{}] is disconnected normally, skip will message", clientId);
            return publishService.clearWill(clientId).then();
        }

        return publishService.findWill(clientId)
            .flatMap(req -> handleWillPublish(clientId, req))
            // 协议要求：无论是否发送，该会话的遗嘱消息都应被清除
            .then(publishService.clearWill(clientId).then());
    }


    /**
     *
     * @author zhouhs
     * @param: clientId
     * @param: req
     * @return: reactor.core.publisher.Mono<java.lang.Boolean>
     **/

    public Mono<Boolean> handleWillPublish(String clientId, TlMqttPublishReq req) {
        Integer willDelay = req.getVariableHead().getWillDelayInterval();
        log.debug("【TLMQTT】handle Will message for client: [{}]", clientId);
        // 逻辑判定：是否需要延迟
        if (willDelay == null || willDelay <= 0) {

            return executePublishToSubscribers(req).thenReturn(true);
        } else {
            log.debug("【TLMQTT】Will message for client: [{}] is delayed 【{}】", clientId,willDelay);
            // 延迟发送：存入调度器，Key 使用 clientId:WILL
            return schedulerTaskService.schedule(clientId + Constant.WILL,
                    executePublishToSubscribers(req),
                    willDelay,
                    TimeUnit.SECONDS)
                .thenReturn(true);
        }
    }

    private Mono<Void> executePublishToSubscribers(TlMqttPublishReq originalReq) {
        String topic = originalReq.getVariableHead().getTopic();
        return subscriptionService.find(topic)
            .flatMap(subClient -> {
                String clientId = subClient.getClientId();
                return sessionService.find(clientId)
                    .flatMap(session -> {
                        Channel channel = channelService.getChannel(clientId);
                        if (channel == null || !channel.isActive()) {
                            return Mono.empty();
                        }

                        MqttQoS realQos = MqttQoS.valueOf(Math.min(
                            originalReq.getFixedHead().getQos().value(),
                            subClient.getQos()
                        ));

                        TlMqttPublishReq targetReq = forwardMessageService.buildTargetReq(
                            originalReq, realQos, session, subClient
                        );
                        // 立刻发送
                        log.debug("【TLMQTT】Will message for client: [{}], topic: [{}], qos: [{}], payload: [{}]",
                            clientId, topic, realQos, targetReq.getPayload());
                        return Mono.create(sink -> channel.writeAndFlush(targetReq).addListener(f -> {
                            if (f.isSuccess()) {
                                sink.success();
                            } else {
                                sink.error(f.cause());
                            }
                        }));
                    })
                    .onErrorResume(e -> Mono.empty()); // 屏蔽单个客户端错误
            }).then();
    }

    private boolean isNormalDisconnect(Channel channel) {
        Boolean flag = (Boolean) channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).get();
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 统一处理响应报文并关闭连接
        handleException(ctx, cause);
    }

    private void handleException(ChannelHandlerContext ctx, Throwable cause) {

        log.error("Exception caught: ", cause);
        if (!(cause instanceof TlMqttException)) {
            ctx.close();
            return;
        }

        TlMqttException ex = (TlMqttException) cause;
        MqttErrorCode errorCode = ex.getErrCode();
        MqttMessageType responseType = ex.getReplayType();
        // 如果是 CONNECT 阶段报错，回复 CONNACK；否则回复 DISCONNECT
        if (responseType == MqttMessageType.CONNACK) {
            TlMqttConnackAck ack = TlMqttConnackAck.build(0, errorCode, MqttVersion.MQTT_5, null, (short) 0,mqttConfiguration);
            ctx.writeAndFlush(ack).addListener(ChannelFutureListener.CLOSE);
        } else if(responseType == MqttMessageType.PUBACK){
            Long messageId = ex.getMessageId();
            TlMqttPubAck req = TlMqttPubAck.build(messageId, errorCode.byteValue(), null, null, MqttVersion.MQTT_5);
            ctx.writeAndFlush(req).addListener(f->{
                if(f.isSuccess() && ex.getClose()){
                    ctx.close();
                }
            });
        }else if(responseType == MqttMessageType.PUBREL){
            Long messageId = ex.getMessageId();
            TlMqttPubRelReq req = TlMqttPubRelReq.build(messageId,errorCode.byteValue());
            ctx.writeAndFlush(req).addListener(f->{
                if(f.isSuccess() && ex.getClose()){
                    ctx.close();
                }
            });
        }else if(responseType == MqttMessageType.DISCONNECT){
            TlMqttDisconnectReq disco = TlMqttDisconnectReq.build(errorCode);
            ctx.writeAndFlush(disco).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (!(evt instanceof IdleStateEvent)) {
            ctx.fireUserEventTriggered(evt);
            return;
        }
        Channel channel = ctx.channel();
        // 标记为非正常断开
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);
    }
}
