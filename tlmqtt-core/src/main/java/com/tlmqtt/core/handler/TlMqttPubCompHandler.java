package com.tlmqtt.core.handler;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
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
 * @Description: comp处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttPubCompHandler extends SimpleChannelInboundHandler<TlMqttPubCompReq> {

    private final TlStoreManager messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubCompReq msg) throws Exception {
        log.debug("in【PUBCOMP】 handler");
        Object clientId = ctx.channel().attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get();
        if (clientId == null) {
            ctx.channel().close();
            return;
        }
        TlMqttPubCompVariableHead variableHead = msg.getVariableHead();

        Long messageId = variableHead.getMessageId();
        log.debug("broker receive client 【{}】 messageId 【{}】 COMP", clientId, messageId);
        //删除rel消息
        messageService.getPubrelService().clear(clientId.toString(), messageId).subscribe(e->{
            //取消定时器发送rel消息
            messageService.getRetryService().cancel(messageId);
        });
    }
}
