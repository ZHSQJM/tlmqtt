package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import com.tlmqtt.core.TlStoreManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2024/12/5 9:46
 * @Description: rel消息处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttPubRelHandler extends SimpleChannelInboundHandler<TlMqttPubRelReq> {

    private final TlStoreManager messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubRelReq req) throws Exception {
        log.debug("in【PUBREL】 handler");
        //根据这个消息获取到对应的
        TlMqttPubRelVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        TlMqttPublishReq publishReq =(TlMqttPublishReq) ctx.channel().attr(AttributeKey.valueOf(Constant.PUB_MSG)).getAndSet(null);
        log.info("broker receive messageId 【{}】",messageId);
        //向客户端发送comp消息
        sendComp(messageId,ctx);
        //转发消息给其他客户端
        TlMqttFixedHead fixedHead = publishReq.getFixedHead();
        TlMqttPublishPayload payload = publishReq.getPayload();
        MqttQoS messageQos = fixedHead.getQos();
        String topic = publishReq.getVariableHead().getTopic();
        String content = payload.getContent().toString();
        messageService.publish(topic,messageQos,content);
    }

    /**
     * 发送comp消息给客户端 表示broker已经收到了消息
     * @param messageId 消息ID
     * @param ctx 通道
     */
    private void sendComp(Long messageId,ChannelHandlerContext ctx){
        log.debug("broker send comp message 【{}】 to client",messageId);
        TlMqttPubCompReq res = TlMqttPubCompReq.build(messageId);
        ctx.channel().writeAndFlush(res);
    }


}
