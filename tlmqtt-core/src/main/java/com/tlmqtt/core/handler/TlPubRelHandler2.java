//package com.tlmqtt.core.handler;
//
//import com.tlmqtt.common.Constant;
//import com.tlmqtt.common.enums.MqttVersion;
//import com.tlmqtt.common.enums.PubReasonCode;
//import com.tlmqtt.common.model.TlMqttSession;
//import com.tlmqtt.common.model.request.TlMqttPubCompReq;
//import com.tlmqtt.common.model.request.TlMqttPubRelReq;
//import com.tlmqtt.common.model.request.TlMqttPublishReq;
//import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
//import com.tlmqtt.core.service.ForwardMessageService;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.util.AttributeKey;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * @author hszhou
// */
//@Slf4j
//@ChannelHandler.Sharable
//public class TlPubRelHandler2 extends AbstractTlHandler<TlMqttPubRelReq> {
//
//    private final ForwardMessageService forwardMessageService;
//
//    public TlPubRelHandler2(ForwardMessageService forwardMessageService) {
//
//        this.forwardMessageService = forwardMessageService;
//    }
//
//    @Override
//    public void handle(ChannelHandlerContext ctx, TlMqttPubRelReq req, TlMqttSession session) {
//        String clientId = session.getClientId();
//        MqttVersion mqttVersion = session.getMqttVersion();
//        //根据这个消息获取到对应的
//        TlMqttPubRelVariableHead variableHead = req.getVariableHead();
//        Long messageId = variableHead.getMessageId();
//        TlMqttPublishReq publishReq = (TlMqttPublishReq) ctx.channel().attr(AttributeKey.valueOf(Constant.PUB_MSG))
//            .getAndSet(null);
//        //向客户端发送comp消息
//        sendComp(messageId, ctx,mqttVersion);
//        //转发消息给其他客户端
//        forwardMessageService.publish(publishReq, clientId,mqttVersion);
//    }
//
//    /**
//     * 发送comp消息给客户端 表示broker已经收到了消息
//     *
//     * @param messageId 消息ID
//     * @param ctx 通道
//     */
//    private void sendComp(Long messageId, ChannelHandlerContext ctx, MqttVersion mqttVersion) {
//        TlMqttPubCompReq res = TlMqttPubCompReq.build(messageId, PubReasonCode.SUCCESS.getCode(), null,null,mqttVersion);
//        ctx.channel().writeAndFlush(res);
//    }
//
//
//}