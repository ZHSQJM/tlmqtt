package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.entity.PubrelMessage;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryMessage;
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
 * @Description: rec消息处理器
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPubRecEventHandler extends SimpleChannelInboundHandler<TlMqttPubRecReq> {

    private final TlStoreManager storeManager;

    private final RetryManager retryManager;



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPubRecReq req) throws Exception {

        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("【Client】5. Handling 【PUBREC】 event from client:【{}】", clientId);

        TlMqttPubRecVariableHead vh = req.getVariableHead();
        Long messageId = vh.getMessageId();

        //收到rec的消息 说明接受方已经收到了发送放的publish消息了 可以这个publish消息删除了
        storeManager.getPublishService().clear( clientId, messageId).subscribe();
        //发送REL消息 告诉发送方 可以发送COMP消息了
        TlMqttPubRelReq res = TlMqttPubRelReq.build(messageId);

        PubrelMessage pubrelMessage = new PubrelMessage();
        pubrelMessage.setMessageId(messageId);
        pubrelMessage.setClientId( clientId);
        //保存rel的消息 是为了cleansession为0的时候 将这个数据发生给客户端
        ctx.channel().writeAndFlush(res);
        storeManager.getPubrelService().save( clientId, messageId, pubrelMessage).subscribe(e -> {
            log.debug("【Client】6. cancel task for publish:【{}】", messageId);
            retryManager.cancel(messageId);
            //定时任务发送rel消息 收到comp后取消
            log.debug("【Client】7. task for pubrel:【{}】", messageId);
            retryManager.retry(new TlRetryMessage(messageId, res), clientId, MqttMessageType.PUBREL);

        });
    }
}