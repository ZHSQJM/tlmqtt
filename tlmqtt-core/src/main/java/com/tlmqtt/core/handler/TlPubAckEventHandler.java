package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
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
 * @Description: ack时间处理器
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubAckEventHandler extends SimpleChannelInboundHandler<TlMqttPubAckReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;




    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubAckReq req) throws Exception {

        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【PubAck】 event from client:【{}】", clientId);
        TlMqttPubAckVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        storeManager.getPublishService().clear(clientId, messageId)
            .subscribe(e -> retryManager.cancel(messageId));
    }
}