package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import com.tlmqtt.core.message.TlMessageService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2025/6/5 18:57
 * @Description: rel消息处理器
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubRelEventHandler extends SimpleChannelInboundHandler<TlMqttPubRelReq> {

    private final TlMessageService messageService;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubRelReq req) throws Exception {

        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("【broker】4. Handling 【PUBREL】 event from client:【{}】",clientId);
        //根据这个消息获取到对应的
        TlMqttPubRelVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        TlMqttPublishReq publishReq = (TlMqttPublishReq) ctx.channel().attr(AttributeKey.valueOf(Constant.PUB_MSG))
            .getAndSet(null);
        //向客户端发送comp消息
        sendComp(messageId, ctx);
        //转发消息给其他客户端
        TlMqttFixedHead fixedHead = publishReq.getFixedHead();
        TlMqttPublishPayload payload = publishReq.getPayload();
        MqttQoS messageQos = fixedHead.getQos();
        String topic = publishReq.getVariableHead().getTopic();
        String content = payload.getContent().toString();
        messageService.publish(topic, messageQos, content);

    }


    /**
     * 发送comp消息给客户端 表示broker已经收到了消息
     *
     * @param messageId 消息ID
     * @param ctx 通道
     */
    private void sendComp(Long messageId, ChannelHandlerContext ctx) {
        log.debug("broker send comp message 【{}】 to client", messageId);
        TlMqttPubCompReq res = TlMqttPubCompReq.build(messageId);
        ctx.channel().writeAndFlush(res);
    }


}