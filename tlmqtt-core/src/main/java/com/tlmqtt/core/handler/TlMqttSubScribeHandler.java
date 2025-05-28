package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.core.TlStoreManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: 订阅主题
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttSubScribeHandler extends SimpleChannelInboundHandler<TlMqttSubscribeReq> {

    private final TlStoreManager messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttSubscribeReq msg) throws Exception {
        log.debug("in【SUBSCRIBE】handler");
        Channel channel = ctx.channel();
        List<TlTopic> topics = msg.getPayload().getTopics();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();

        messageService.getSessionService().find(clientId).flatMap(session -> {
            Set<String> subTopics = session.getTopics();
            Set<String> collect = topics.stream().map(TlTopic::getName).collect(Collectors.toSet());
            //移除所有与传入主题同名的旧主题
            collect.forEach(subTopics::remove);
            // 添加所有新主题 （自动去重）
            subTopics.addAll(collect);
            //发送订阅确认
            int messageId = msg.getVariableHead().getMessageId();
            TlMqttSubAck res = TlMqttSubAck.of(topics, messageId);
            channel.writeAndFlush(res);
            return messageService.getSessionService().save(session);
        }).thenMany(  Flux.fromIterable(topics).flatMap(topic -> {
            TlSubClient client = new TlSubClient(topic.getQos(), clientId, topic.getName());
            //找到主题的保留消息 然后发送  将订阅关系保存
            return messageService.getRetainService().find(topic.getName()).doOnNext(e->{
                log.debug("send retain message 【{}】 to client 【{}】",e.toString(),clientId);
                TlMqttPublishReq message = TlMqttPublishReq.build(topic.getName(), MqttQoS.valueOf(e.getQos()), true, e.getMessage(), e.getMessageId());
                channel.writeAndFlush(message);
            }).then(messageService.getSubscriptionService().subscribe(client)
                .doOnNext(e->{
                    log.debug("client 【{}】 subscribe topic 【{}】",clientId,  topics);
                }));
        }))
      .subscribe();


    }
}
