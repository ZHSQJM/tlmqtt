package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlMalformedPacketException;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.exception.TopicAliasInvalidException;
import com.tlmqtt.common.exception.UnAcceptableProtocolVersionException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.common.model.variable.TlMqttDisconnectVariableHead;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.MessageManager;
import com.tlmqtt.core.task.TlWillTask;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final MessageManager messageManager;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

        log.info("进入inactive的模块");
        Channel channel = ctx.channel();
        Object obj = channel.attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        if (obj == null) {
            return;
        }
        TlMqttSession session = (TlMqttSession) obj;
        String clientId = session.getClientId();
        //检查channel管理器中的通道是否是当前通道 如果不是 那么就不需要做任何事情  这种情况是重复clientId的发生 将上一个通道关闭即可
        Channel currentChannel = channelManager.getChannel(clientId);
        if (currentChannel != channel) {
            log.info("Channel for client:【{}】 has been replaced, skip cleanup", clientId);
            return;
        }
        MqttVersion mqttVersion = session.getMqttVersion();
        handleWillMessage(clientId, mqttVersion)
            .then(handleSessionCleanup(session))
            .doFinally(signalType -> channel.close())
            .subscribe();
    }

    private Mono<Void> handleSessionCleanup(TlMqttSession session) {
        String clientId = session.getClientId();
        channelManager.remove(clientId);
        boolean cleanSession = session.isCleanSession();
        //CleanStart=true：丢弃任何现有会话，建立全新会话（类似 MQTT 3.1.1 的 CleanSession=true）。
        if (cleanSession) {
            return storeManager.clearAll(clientId);
        }

        MqttVersion mqttVersion = session.getMqttVersion();
        //定义会话在断开连接后的保留时间（单位：秒）。
        //Session Expiry Interval=0（默认值）：会话在断开时立即删除
        //Session Expiry Interval>0：会话保留指定时间，客户端在此期间重连可恢复订阅和未接收的 QoS 1/2 消息。
        //Session Expiry Interval=0xFFFFFFFF（无限）：会话永久保留（类似 MQTT 3.1.1 的 CleanSession=false 但无时间限制）135。
        if (mqttVersion == MqttVersion.MQTT_5) {
            int sessionExpiryInterval = session.getSessionExpiryInterval();
            if (sessionExpiryInterval == 0) {
                return storeManager.clearAll(clientId);
            }
           return storeManager.scheduleRemoveSession(session);
        } else {
            return Mono.empty();
        }
    }

    /**
     * 处理遗嘱消息
     * 1. 服务端检测到了一个I/O错误或者网络故障。
     * 2. 客户端在保持连接（Keep Alive）的时间内未能通讯。
     * 3. 客户端没有先发送原因码为 0x00 （正常断连）的 DISCONNECT 报文直接关闭了网络连接。
     * 4. 服务器没有先发送原因码为 0x00 （正常断连）的 DISCONNECT 报文直接关闭了网络连接。
     * @param clientId 客户端的ID
     * @return 是否发送遗嘱消息成功
     *
     * todo 当连接断开后，尽管会话还会保持，无论遗嘱消息是否发生，该条遗嘱消息不应该存在了
     */
    private Mono<Boolean> handleWillMessage(String clientId, MqttVersion version) {

        if (isNormalDisconnect(clientId)) {
            return storeManager.getPublishService()
                               .clearWill(clientId);
        }
        return storeManager.getPublishService()
            .findWill(clientId)
            .doOnNext(req -> log.info("获取遗嘱消息【{}】", req))
            .flatMap(req -> {
                log.info("获取到遗嘱消息【{}】", req);
                TlMqttPublishVariableHead variableHead = req.getVariableHead();
                Integer willDelayInterval = variableHead.getWillDelayInterval();
                if (willDelayInterval == null) {
                    log.error("WillDelayInterval为空");
                   return publishToSubscribers(req, clientId, version);
                }
                log.info("WillDelayInterval【{}】", willDelayInterval);
                return messageManager.scheduleSendWillMessage(clientId, req, willDelayInterval)
                    .then(Mono.empty());
            });
    }

    private Mono<Boolean> publishToSubscribers(TlMqttPublishReq req, String clientId, MqttVersion version) {
        messageManager.publish(req, clientId, version);
        return Mono.empty();
    }



    /**
     * 判定是否是正常判断
     * @param clientId 客户端ID
     * @return 是否正常断开
     */
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
        log.error("异常[{}]", cause.getClass());
        ReferenceCountUtil.release(cause);


        /*当服务端检测到无效报文或协议错误，并且本规范中给出了相应的原因码时，它必须关闭网络连接 [MQTT-4.13.1-1]。
         * 在CONNECT报文出错的情况下它可以在关闭网络连接之前发送包含原因码的CONNACK报文。
         * 在其他报文出错的情况下它应该在关闭网络连接之前发送包含原因码的DISCONNECT报文。
         * 使用原因码0x81（无效报文）或0x82（协议错误），除非包含3.2.2.2节 - 连接原因码 或3.14.2.1节 – 断开原因码 中定义的更具体的原因码。对其他会话没有影响*/
        if (cause instanceof TlProtocolErrorException) {
            log.info("协议错误");
            TlProtocolErrorException ex = (TlProtocolErrorException) cause;
            MqttMessageType mqttMessageType = ex.getMqttMessageType();
            if (mqttMessageType == MqttMessageType.CONNECT) {
                TlMqttConnackAck req = TlMqttConnackAck.build(0, ex.getErrCode(), MqttVersion.MQTT_5, null, (short) 0);
                ctx.channel().writeAndFlush(req);
            } else {
                TlMqttDisconnectReq req = TlMqttDisconnectReq.build(ex.getErrCode());
                ctx.channel().writeAndFlush(req);
            }
            ctx.close();
        } else if (cause instanceof TlMalformedPacketException) {
            log.error("无效报文");
            TlMalformedPacketException ex = (TlMalformedPacketException) cause;
            MqttMessageType mqttMessageType = ex.getMqttMessageType();
            if (mqttMessageType == MqttMessageType.CONNECT) {
                TlMqttConnackAck req = TlMqttConnackAck.build(0, ex.getErrorCode(), MqttVersion.MQTT_5, null, (short) 0);
                ctx.channel().writeAndFlush(req);
            } else {
                TlMqttDisconnectReq req = TlMqttDisconnectReq.build(ex.getErrorCode());
                ctx.channel().writeAndFlush(req);
            }
            ctx.close();
        } else if (cause instanceof UnAcceptableProtocolVersionException) {
            log.info("不支持协议版本");
            UnAcceptableProtocolVersionException ex = (UnAcceptableProtocolVersionException) cause;
            TlMqttConnackAck req = TlMqttConnackAck.build(0, ex.getErrCode(), MqttVersion.MQTT_5, null, (short) 0);
            ctx.channel().writeAndFlush(req);
            ctx.close();
        } else if (cause instanceof TlMqttException) {
            TlMqttException ex = (TlMqttException) cause;
            log.info("[{}]",ex);
            MqttMessageType send = ex.getSend();
            if(send == MqttMessageType.CONNACK){
                TlMqttConnackAck req = TlMqttConnackAck.build(0, ex.getErrCode(), MqttVersion.MQTT_5, null, (short) 0);
                ctx.channel().writeAndFlush(req).addListener(future -> {
                    if(ex.getClose()){
                        log.error("MQTT异常，关闭连接: {}", ex.getErrCode());
                        ctx.close();
                    }
                });
            }else if(send ==MqttMessageType.DISCONNECT){
                TlMqttDisconnectReq req = TlMqttDisconnectReq.build(ex.getErrCode());
                ctx.channel().writeAndFlush(req).addListener(future -> {
                    log.error("MQTT异常，关闭连接: {}", ex.getErrCode());
                    ctx.close();
                });
            }
        } else if(cause instanceof  TopicAliasInvalidException){
            log.error("TopicAliasInvalidException: {}", cause.getMessage());
            TopicAliasInvalidException ex = ((TopicAliasInvalidException) cause);
        }else if (cause instanceof SocketException) {
            log.error("Socket异常，关闭连接");
            ctx.close();
        } else {
            log.error("未知异常，关闭连接");
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
