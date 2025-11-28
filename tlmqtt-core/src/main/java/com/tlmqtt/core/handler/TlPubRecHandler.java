package com.tlmqtt.core.handler;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.task.TlRetryTask;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubRecHandler extends AbstractTlHandler<TlMqttPubRecReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;

    private final ExecutorService  executorService;

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubRecReq req, TlMqttSession session) {

        String clientId = session.getClientId();
        TlMqttPubRecVariableHead vh = req.getVariableHead();
        Long messageId = vh.getMessageId();
        log.debug("Handling 【PUBREC】 event from client:【{}】,message=[{}]", clientId,messageId);
        retryManager.cancelPublishRetry(messageId);
        storeManager.getPublishService()
            .clear(clientId, messageId)
            .flatMap(publishReq -> {
                log.info("找到消息【{}】",publishReq);
                TlMqttPubRelReq res = TlMqttPubRelReq.build(messageId);
                return storeManager.getPubrelService().save(clientId, messageId, res);
            })
            .subscribe(relReq -> {
                // 发送操作回到Netty线程
                log.info("发送rel消息");
                ctx.channel().eventLoop().execute(() -> {
                    ctx.channel().writeAndFlush(relReq);
                    TlRetryTask tlRetryTask = new TlRetryTask(messageId, relReq, ctx.channel());
                    retryManager.schedulePubrelRetry(messageId, tlRetryTask);

                });
            });
    }
}