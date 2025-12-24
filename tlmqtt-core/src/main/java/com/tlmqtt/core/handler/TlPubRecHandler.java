package com.tlmqtt.core.handler;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import com.tlmqtt.core.manager.RetryManager;

import com.tlmqtt.core.task.TlRetryTask;

import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPubRecHandler extends AbstractTlHandler<TlMqttPubRecReq> {
    private final PubrelService pubrelService;

    public TlPubRecHandler(PublishService publishService,
        PubrelService pubrelService,
        RetryManager retryManager) {
        super.setPublishService(publishService);
        super.setRetryManager(retryManager);
        this.pubrelService = pubrelService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubRecReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        long messageId = req.getVariableHead().getMessageId();

        log.debug("Received PUBREC from client: [{}], messageId: [{}]", clientId, messageId);

        // 1. 立即停止 PUBLISH 报文的重试 (第一阶段结束)
        retryManager.cancelPublishRetry(messageId);

        // 2. 逻辑链条：清除 PUBLISH 存储 -> 构建并保存 PUBREL -> 发送并启动 PUBREL 重试
        publishService
            .clear(clientId, messageId) // 清理第一阶段消息
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(oldPubReq -> {
                // 协议要求：即便没找到原消息（例如由于意外重启），也要回复 PUBREL
                TlMqttPubRelReq relReq = TlMqttPubRelReq.build(messageId);
                return pubrelService.save(clientId, messageId, relReq);
            })
            .doOnError(e -> log.error("Error processing PUBREC for client [{}], id [{}]", clientId, messageId, e))
            .subscribe(relReq -> {
                // 3. 执行发送并开启第二阶段重试
                ctx.channel().eventLoop().execute(() -> {
                    ctx.writeAndFlush(relReq).addListener(future -> {
                        if (future.isSuccess()) {
                            // 开启针对 PUBREL 的重试任务
                            TlRetryTask retryTask = new TlRetryTask(messageId, relReq, ctx.channel());
                            retryManager.schedulePubrelRetry(messageId, retryTask);
                            log.debug("Sent PUBREL to [{}], msgId: [{}]", clientId, messageId);
                        }
                    });
                });
            });
    }
}