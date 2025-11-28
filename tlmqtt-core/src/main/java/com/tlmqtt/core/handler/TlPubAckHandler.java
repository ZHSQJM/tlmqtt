package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
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

import java.util.concurrent.locks.LockSupport;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubAckHandler extends AbstractTlHandler<TlMqttPubAckReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;

    private final MessageManager messageManager;

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubAckReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        log.debug("Handling 【PubAck】 event from client:【{}】", clientId);
        TlMqttPubAckVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        retryManager.cancelPublishRetry(messageId);
        messageManager.ack(clientId);
        storeManager.getPublishService()
            .clear(clientId, messageId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
}