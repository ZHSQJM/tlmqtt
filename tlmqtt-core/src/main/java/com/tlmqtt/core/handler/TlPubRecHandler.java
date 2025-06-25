package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.entity.PubrelMessage;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubRecHandler extends SimpleChannelInboundHandler<TlMqttPubRecReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;

    private final ExecutorService  executorService;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubRecReq req) throws Exception {

        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【PUBREC】 event from client:【{}】", clientId);

        TlMqttPubRecVariableHead vh = req.getVariableHead();
        Long messageId = vh.getMessageId();
        retryManager.cancelPublishRetry(messageId);
        executorService.execute(()-> storeManager.getPublishService()
            .clear(clientId, messageId)
            .flatMap(e -> {
                PubrelMessage pubrelMessage = PubrelMessage.builder()
                    .messageId(messageId)
                    .clientId(clientId)
                    .build();
                return storeManager.getPubrelService().save(clientId, messageId, pubrelMessage);
            })
            .subscribe(e -> {
                // 发送操作回到Netty线程
                channel.eventLoop().execute(() -> {
                    TlMqttPubRelReq res = TlMqttPubRelReq.build(messageId);
                    channel.writeAndFlush(res);

                    TlRetryTask tlRetryTask = new TlRetryTask(messageId, res, channel);
                    retryManager.schedulePubrelRetry(messageId, tlRetryTask);
                });
            }));

    }
}