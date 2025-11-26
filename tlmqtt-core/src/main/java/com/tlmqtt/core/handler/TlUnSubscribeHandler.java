package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
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
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlUnSubscribeHandler extends AbstractTlHandler<TlMqttUnSubscribeReq> {

    private final TlStoreManager storeManager;



    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttUnSubscribeReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        log.debug("Handling 【UNSUBSCRIBE】 event from client:【{}】", clientId);
        TlMqttUnSubscribePayload payload = req.getPayload();
        List<TlTopic> topics = payload.getTopics();
        //取消订阅 就移除掉主题对应的客户端 这样发送消息就接收不到
        topics.forEach(topic-> storeManager.getSubscriptionService()
            .unsubscribe(clientId, topic.getName())
            .subscribe(e -> log.debug("Client 【{}】 unsubscribe topic 【{}】", clientId, topic)));
        //构建ack消息
        int messageId = req.getVariableHead().getMessageId();
        TlMqttUnSubAck res = TlMqttUnSubAck.build(messageId);
        ctx.channel().writeAndFlush(res);
    }
}