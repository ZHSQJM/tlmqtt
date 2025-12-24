package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlExceptionHandler extends ChannelInboundHandlerAdapter {



    private final PublishService publishService;
    private final ChannelManager channelManager;
    private final SessionService sessionService;
    private final MqttConfiguration mqttConfiguration;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        TlMqttSession session = (TlMqttSession) channel.attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();

        if (session == null) {
            ctx.close();
            return;
        }

        String clientId = session.getClientId();
        // 1. 判定是否为“冲突剔除”。如果是，不触发遗嘱和清理逻辑
        if (channelManager.getChannel(clientId) != channel) {
            log.info("Channel for client: [{}] has been replaced, skip cleanup", clientId);
            return;
        }

        // 2. 串行处理：遗嘱 -> 会话清理 -> 关闭通道
        handleWillMessage(session)
            .then(handleSessionCleanup(session))
            .doFinally(signalType -> {
                channelManager.remove(clientId);
                ctx.close();
            })
            .subscribe(
                null,
                e -> log.error("Error during channel inactive cleanup for [{}]: {}", clientId, e.getMessage())
            );
    }

    private Mono<Void> handleSessionCleanup(TlMqttSession session) {
        String clientId = session.getClientId();
        MqttVersion version = session.getMqttVersion();

        // MQTT 3.1.1 逻辑
        if (version != MqttVersion.MQTT_5) {
            return session.isCleanSession()
                ? sessionService.clearAll(clientId).then()
                : Mono.empty();
        }

        // MQTT 5.0 逻辑
        int expiryInterval = session.getSessionExpiryInterval();
        if (expiryInterval <= 0) {
            // 立即清理
            return sessionService.clearAll(clientId).then();
        } else {
            // 延时清理：利用我们之前实现的 Caffeine 机制
            return sessionService.scheduleRemoval(clientId, expiryInterval);
        }
    }

    private Mono<Void> handleWillMessage(TlMqttSession session) {
        String clientId = session.getClientId();

        // 如果是正常断开 (发送了 DISCONNECT 报文)，则取消遗嘱
        if (isNormalDisconnect(session.getCtx().channel())) {
            return publishService.clearWill(clientId).then();
        }

        return publishService.findWill(clientId)
            .flatMap(req -> {
                Integer willDelay = req.getVariableHead().getWillDelayInterval();
                // 如果没有设置延时，或者不是 MQTT 5.0，立即发送
                if (willDelay == null || willDelay == 0) {
                    return publishToSubscribers(req, clientId, session.getMqttVersion()).then();
                }
                // TODO: 实现 Will Delay 延时任务逻辑
                log.info("Will message for [{}] delayed by {}s", clientId, willDelay);
                return Mono.empty();
            })
            // 协议要求：无论是否发送，该会话的遗嘱消息都应被清除
            .then(publishService.clearWill(clientId).then());
    }

    private Mono<Boolean> publishToSubscribers(TlMqttPublishReq req, String clientId, MqttVersion version) {
        log.info("Publishing Will Message for client: [{}]", clientId);
        // 这里应调用你的消息分发逻辑（Dispatcher）
        return Mono.just(true);
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
        // 释放可能存在的 ByteBuf 引用
        if (!(cause instanceof TlMqttException)) {
            log.error("Unhandled System Exception: ", cause);
        }

        MqttErrorCode errorCode = MqttErrorCode.MALFORMED_MESSAGE;
        MqttMessageType responseType = MqttMessageType.DISCONNECT;

        if (cause instanceof TlMqttException) {
            TlMqttException ex = (TlMqttException) cause;
            errorCode = ex.getErrCode();
            responseType = ex.getReplayType();
        }

        // 如果是 CONNECT 阶段报错，回复 CONNACK；否则回复 DISCONNECT
        if (responseType == MqttMessageType.CONNACK || responseType == MqttMessageType.CONNECT) {
            TlMqttConnackAck ack = TlMqttConnackAck.build(0, errorCode, MqttVersion.MQTT_5, null, (short) 0,mqttConfiguration);
            ctx.writeAndFlush(ack).addListener(ChannelFutureListener.CLOSE);
        } else if(responseType == MqttMessageType.PUBACK){

        }
        else {
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
