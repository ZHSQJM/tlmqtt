package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;
import com.tlmqtt.core.manager.TlStoreManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2025/6/5 18:55
 * @Description: UnSubscribe消息处理器
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlUnSubscribeEventHandler extends SimpleChannelInboundHandler<TlMqttUnSubscribeReq> {

    private final TlStoreManager storeManager;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttUnSubscribeReq req) throws Exception {
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【UNSUBSCRIBE】 event from client:【{}】", clientId);


        TlMqttUnSubscribePayload payload = req.getPayload();
        List<TlTopic> topics = payload.getTopics();
        //取消订阅 就移除掉主题对应的客户端 这样发送消息就接收不到
        topics.forEach(topic-> storeManager.getSubscriptionService()
            .unsubscribe(clientId, topic.getName())
            .subscribe(e -> log.debug("Client 【{}】 unsubscribe topic 【{}】", clientId, topic)));

        //构建ack消息
        int messageId = req.getVariableHead().getMessageId();
        TlMqttUnSubAck res = TlMqttUnSubAck.of(messageId);
        ctx.channel().writeAndFlush(res);
    }
}