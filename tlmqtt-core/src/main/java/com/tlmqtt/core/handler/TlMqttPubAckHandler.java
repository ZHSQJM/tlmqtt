package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import com.tlmqtt.core.TlStoreManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: ack回复处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttPubAckHandler extends SimpleChannelInboundHandler<TlMqttPubAckReq> {

    private final TlStoreManager messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubAckReq msg) throws Exception {

        log.debug("in 【PUBACK】 handler");
        Object clientId = ctx.channel().attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get();
        if(clientId==null){
            ctx.channel().close();
            return;
        }
        TlMqttPubAckVariableHead variableHead = msg.getVariableHead();
        Long messageId = variableHead.getMessageId();
        log.debug("receive client 【{}】 messageId 【{}】 ack",clientId,messageId);


        messageService.getPublishService().clear(clientId.toString(),messageId).subscribe(
              e-> messageService.getRetryService().cancel(messageId)
        );
    }
}
