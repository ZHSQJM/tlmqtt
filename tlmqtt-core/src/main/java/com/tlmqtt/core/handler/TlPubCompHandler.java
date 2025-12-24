package com.tlmqtt.core.handler;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.store.service.PubrelService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlPubCompHandler extends AbstractTlHandler<TlMqttPubCompReq> {



    private final ForwardMessageService forwardMessageService;

    public TlPubCompHandler(ForwardMessageService forwardMessageService,
        RetryManager retryManager,
        PubrelService pubrelService) {
        this.forwardMessageService = forwardMessageService;
        super.setRetryManager(retryManager);
        super.setPubrelService(pubrelService);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubCompReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        // 强制转换为 int 以适配 MessageIdManager 的 16 位逻辑
        int messageId = req.getVariableHead().getMessageId().intValue();

        log.debug("Received PUBCOMP from client: [{}], messageId: [{}]", clientId, messageId);

        // 1. 立即停止 PUBREL 的重试定时器
        retryManager.cancelPubrelRetry((long) messageId);

        // 2. 核心：通过 handleAck 释放 ID、减小 In-Flight 计数、驱动队列
        // 这里的 handleAck 逻辑与 QoS 1 的 PUBACK 处理逻辑一致，实现了资源的回收
        forwardMessageService.handleAck(clientId, messageId);

        // 3. 异步清理持久化的 PUBREL 记录
        pubrelService
            .clear(clientId, (long) messageId)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Failed to clear PUBREL for client [{}], id [{}]", clientId, messageId, e))
            .subscribe();
    }
}