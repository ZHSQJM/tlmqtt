package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import com.tlmqtt.core.manager.MessageManager;
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
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class TlPubCompHandler extends AbstractTlHandler<TlMqttPubCompReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;

    private final MessageManager messageManager;


    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubCompReq req, TlMqttSession session) {

        String clientId = session.getClientId();
        log.debug("Handling 【PUBCOMP】 event from client:【{}】", clientId);
        TlMqttPubCompVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        retryManager.cancelPubrelRetry(messageId);
        messageManager.ack(clientId);
        storeManager.getPubrelService()
            .clear(clientId, messageId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
}