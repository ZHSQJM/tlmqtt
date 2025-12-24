package com.tlmqtt.core.handler;

import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;
import com.tlmqtt.store.service.SubscriptionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlUnSubscribeHandler2 extends AbstractTlHandler<TlMqttUnSubscribeReq> {


    public TlUnSubscribeHandler2(SubscriptionService subscriptionService) {
        super.setSubscriptionService(subscriptionService);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttUnSubscribeReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        log.debug("Handling 【UNSUBSCRIBE】 event from client:【{}】", clientId);
        TlMqttUnSubscribePayload payload = req.getPayload();
        List<TlTopic> topics = payload.getTopics();
        for (TlTopic topic : topics) {

            //校验主题是否正常 否则抛出0x8F的错误
        }
        //取消订阅 就移除掉主题对应的客户端 这样发送消息就接收不到
        topics.forEach(topic-> subscriptionService
            .unsubscribe(clientId, topic.getName())
            .subscribe(e -> log.debug("Client 【{}】 unsubscribe topic 【{}】", clientId, topic)));
        //构建ack消息
        int messageId = req.getVariableHead().getMessageId();
        TlMqttUnSubAck res = TlMqttUnSubAck.build(messageId);
        ctx.channel().writeAndFlush(res);
    }


}