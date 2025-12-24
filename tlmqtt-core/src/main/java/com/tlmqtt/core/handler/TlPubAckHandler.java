package com.tlmqtt.core.handler;


import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.store.service.PublishService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;
/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPubAckHandler extends AbstractTlHandler<TlMqttPubAckReq> {
    private final ForwardMessageService forwardMessageService;

    public TlPubAckHandler(PublishService publishService, RetryManager retryManager, ForwardMessageService forwardMessageService) {
        super.setPublishService(publishService);
        super.setRetryManager(retryManager);
        this.forwardMessageService = forwardMessageService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubAckReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        // MQTT 协议中 MessageId 为 16位无符号整型
        int messageId = req.getVariableHead().getMessageId().intValue();

        log.debug("Received PUBACK from client: [{}], messageId: [{}]", clientId, messageId);

        // 1. 立即停止重试定时器（防止重复发送）
        retryManager.cancelPublishRetry((long) messageId);

        // 2. 核心：通知 ForwardMessageService 完成确认
        // 该方法内部会：1. 释放 ID  2. 减小 In-Flight 计数  3. 触发队列中的下一条消息
        forwardMessageService.handleAck(clientId, messageId);

        // 3. 异步清理持久化的离线消息
        publishService
            .clear(clientId, (long) messageId)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Failed to clear persistent message for client: [{}], id: [{}]", clientId, messageId, e))
            .subscribe();
    }
}