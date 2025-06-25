package com.tlmqtt.core.handler;

import com.tlmqtt.auth.acl.AclManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.core.manager.TlStoreManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlSubscribeHandler extends SimpleChannelInboundHandler<TlMqttSubscribeReq> {

    private final TlStoreManager storeManager;

    private final AclManager aclManager;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttSubscribeReq req) throws Exception {
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【SUBSCRIBE】 event from client:【{}】", clientId);

        List<TlTopic> topics = req.getPayload().getTopics();
        List<TlTopic> successTopic = new ArrayList<>();
        int[] codes = new int[topics.size()];

        storeManager.getSessionService().find(clientId).flatMap(session -> {
            //发送订阅确认
            int messageId = req.getVariableHead().getMessageId();
            for (int i = 0; i < topics.size(); i++) {
                TlTopic tlTopic = topics.get(i);
                if (aclManager.checkSubscribePermission(session, tlTopic.getName())) {
                    codes[i] = tlTopic.getQos();
                    successTopic.add(tlTopic);
                } else {
                    codes[i] = 0x80;
                }
            }
            TlMqttSubAck res = TlMqttSubAck.of(codes, messageId);
            channel.writeAndFlush(res);
            return storeManager.getSessionService().save(session);
        }).onErrorResume(e -> {
            // 3. 捕获异常并返回空流，防止进入 thenMany
            log.debug("Subscription aborted due to error: {}", e.getMessage());
            return Mono.empty();
        }).thenMany(Flux.fromIterable(successTopic).flatMap(topic -> {
            TlSubClient client = new TlSubClient(topic.getQos(), clientId, topic.getName());
            //找到主题的保留消息
            return storeManager.getRetainService().find(topic.getName()).doOnNext(e -> {
                log.debug("Send retain message 【{}】 to client 【{}】", e.toString(), clientId);
                TlMqttPublishReq message = TlMqttPublishReq.build(topic.getName(), MqttQoS.valueOf(e.getQos()), true,
                    e.getMessage(), e.getMessageId());
                channel.writeAndFlush(message);
            }).then(storeManager.getSubscriptionService().subscribe(client)
                .doOnNext(e -> log.debug("Client 【{}】 subscribe topic 【{}】", clientId, topics)));
        })).subscribe();
    }
}