package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.message.TlMessageService;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.SocketException;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlExceptionHandler extends ChannelInboundHandlerAdapter {


    private final TlStoreManager storeManager;

    private final ChannelManager channelManager;

    private final TlMessageService messageService;





    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Object clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get();
        if (clientId == null) {
            return;
        }
        //检查当前channel是否仍然是channelManager中注册的channel
        Channel currentChannel = channelManager.getChannel(clientId.toString());
        if (currentChannel != channel) {
            log.info("Channel for client:【{}】 has been replaced, skip cleanup", clientId);
            return;
        }
        handleSessionCleanup(clientId.toString()).then(handleWillMessage(clientId.toString())).doFinally(signal -> channel.close())
            .subscribe(null, e -> log.error("Channel inactive processing error", e));
    }

    private Mono<Void> handleSessionCleanup(String clientId) {
        return storeManager.getSessionService().find(clientId).flatMap(session -> {
            channelManager.remove(clientId);
            return session.getCleanSession() ? storeManager.clearAll(clientId) : Mono.empty();
        }).onErrorResume(e -> {
            log.error("Session cleanup error ", e);
            return Mono.empty();
        });
    }

    private Mono<Boolean> handleWillMessage(String clientId) {
        if (isNormalDisconnect(clientId)) {
            return storeManager.getPublishService().clearWill(clientId);
        }
        return storeManager.getPublishService().findWill(clientId).flatMap(willMsg -> {
            TlMqttPublishReq willRequest = buildWillPublishReq(willMsg);
            return publishToSubscribers(willMsg.getTopic(), willRequest).then(
                storeManager.getPublishService().clearWill(clientId));
        }).onErrorResume(e -> {
            log.error("Will message processing error", e);
            return Mono.just(false);
        });
    }

    private Flux<Void> publishToSubscribers(String topic, TlMqttPublishReq message) {
        return storeManager.getSubscriptionService().find(topic).flatMap(subscription -> {
            channelManager.writeAndFlush(subscription.getClientId(), message);
            return Flux.empty();
        });
    }

    private TlMqttPublishReq buildWillPublishReq(PublishMessage willMsg) {
        String topic = willMsg.getTopic();
        MqttQoS qos = MqttQoS.valueOf(willMsg.getQos());
        TlMqttPublishReq req = TlMqttPublishReq.build(topic, qos, false, willMsg.getMessage());
        if (qos != MqttQoS.AT_MOST_ONCE) {
            Long messageId = messageService.nextId();
            req.getVariableHead().setMessageId(messageId);
        }
        return req;
    }

    private boolean isNormalDisconnect(String clientId) {
        Channel channel = channelManager.getChannel(clientId);
        if (channel == null) {
            return false;
        }
        Boolean flag = (Boolean) channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).get();
        return flag != null && flag;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ReferenceCountUtil.release(cause);
        log.error("Exception", cause);
        if (cause instanceof SocketException) {
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (!(evt instanceof IdleStateEvent)) {
            ctx.fireUserEventTriggered(evt);
            return;
        }
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【Heart】 event from client:【{}】", clientId);
        // 标记为非正常断开
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);
    }
}
