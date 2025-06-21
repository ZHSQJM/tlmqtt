package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: comp处理器
 */
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class TlPubCompHandler extends SimpleChannelInboundHandler<TlMqttPubCompReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubCompReq req) throws Exception {
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【PUBCOMP】 event from client:【{}】", clientId);
        TlMqttPubCompVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        retryManager.cancelPubrelRetry(messageId);
        storeManager.getPubrelService()
                    .clear(clientId, messageId)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
    }
}