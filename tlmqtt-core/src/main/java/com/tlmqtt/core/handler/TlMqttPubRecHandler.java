package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.entity.PubrelMessage;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import com.tlmqtt.core.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2024/12/5 9:46
 * @Description: rec消息处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttPubRecHandler extends SimpleChannelInboundHandler<TlMqttPubRecReq> {

    private final TlStoreManager messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubRecReq req) throws Exception {
        log.debug("in【PUBREC】 handler");
        String clientId = ctx.channel().attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        TlMqttPubRecVariableHead vh = req.getVariableHead();
        Long messageId = vh.getMessageId();

        //收到rec的消息 说明接受方已经收到了发送放的publish消息了 可以吧这个publish消息删除了
        //messageService.getPublishService().clear(clientId, messageId).subscribe();
        //发送REL消息 告诉发送方 可以发送COMP消息了
        TlMqttPubRelReq res = TlMqttPubRelReq.build(messageId);

        PubrelMessage pubrelMessage = new PubrelMessage();
        pubrelMessage.setMessageId(messageId);
        pubrelMessage.setClientId(clientId);
        //保存rel的消息 是为了cleansession为0的时候 将这个数据发生给客户端
        ctx.channel().writeAndFlush(res);
        log.info("保存到pubrel的缓存数据是【{}】", pubrelMessage);
        messageService.getPubrelService().save(clientId, messageId, pubrelMessage).subscribe(e -> {
            if (e) {
                //定时任务发送rel消息 收到comp后取消
                messageService.getRetryService().retry(new TlRetryMessage(messageId, res), clientId);
            }
        });

    }
}
