package com.tlmqtt.core.handler;

import com.tlmqtt.auth.acl.AclManager;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.SubReasonCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.manager.MessageManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlSubscribeHandler extends AbstractTlHandler<TlMqttSubscribeReq> {

    private final TlStoreManager storeManager;

    private final AclManager aclManager;

    private final MessageManager messageManager;




    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttSubscribeReq req, TlMqttSession session) {
        Channel channel = ctx.channel();
        MqttVersion mqttVersion = session.getMqttVersion();
        String clientId = session.getClientId();
        //log.debug("Handling 【SUBSCRIBE】 event from client:【{}】", clientId);

        List<TlTopic> topics = req.getPayload().getTopics();
        Set<TlTopic> successTopic = new HashSet<>();
        int[] codes = new int[topics.size()];

        TlMqttSubscribeVariableHead variableHead = req.getVariableHead();
        int messageId = variableHead.getMessageId();
        //发送订阅确认
        for (int i = 0; i < topics.size(); i++) {
            TlTopic tlTopic = topics.get(i);
            if (aclManager.checkSubscribePermission(session, tlTopic.getName())) {
                codes[i] = tlTopic.getQos();
                successTopic.add(tlTopic);
            } else if(mqttVersion==MqttVersion.MQTT_3_1_1){
                codes[i] = SubReasonCode.FAILURE.getCode();
            } else if(mqttVersion==MqttVersion.MQTT_5){
                codes[i] = SubReasonCode.NOT_AUTHORIZED.getCode();
            }
            log.info("【SUBSCRIBE】 event from client:【{}】--【{}】", clientId, tlTopic.getName());
        }
        TlMqttSubAck res = TlMqttSubAck.build(codes, messageId,null,null);


        channel.writeAndFlush(res);

        session.getTopics().addAll(successTopic.stream().map(TlTopic::getName).collect(Collectors.toSet()));
        storeManager.getSessionService()
            .save(session)
            .onErrorResume(e -> {
                // 3. 捕获异常并返回空流，防止进入 thenMany
                log.debug("Subscription aborted due to error: {}", e.getMessage());
                return Mono.empty();
            }).thenMany(Flux.fromIterable(successTopic).flatMap(topic -> {
                int qos= topic.getQos();
                TlSubClient client = new TlSubClient(qos, clientId, topic.getName(),mqttVersion);
                //找到主题的保留消息
                return storeManager.getRetainService().find(topic.getName()).doOnNext(publishReq -> {
                    log.debug("Send retain message 【{}】 to client 【{}】", publishReq.toString(), clientId);
                    TlMqttPublishVariableHead publishReqVariableHead = publishReq.getVariableHead();
                    Integer messageExpiryInterval = publishReqVariableHead.getMessageExpiryInterval();
                    if (MqttVersion.MQTT_5 == mqttVersion && messageExpiryInterval != null) {
                        long currentTime = System.currentTimeMillis()/1000;
                        Long acceptTime = publishReq.getAcceptTime();
                        if (acceptTime + messageExpiryInterval < currentTime) {
                            log.debug("Retain message 【{}】 is expired", publishReq);
                            return;
                        }
                        long remainingTime = messageExpiryInterval - (currentTime - acceptTime);
                        publishReqVariableHead.setMessageExpiryInterval((int) remainingTime);
                    }
                    //这是保留消息的qos等级
                    int retainQos = publishReq.getFixedHead().getQos().value();
                    int realQos = Math.min(qos, retainQos);
                    MqttQoS mqttQoS = MqttQoS.valueOf(realQos);
                    TlMqttPublishReq publishMessage = messageManager.build(publishReq, mqttQoS,mqttVersion);
                    if(mqttQoS != MqttQoS.AT_MOST_ONCE){
                        storeManager.savePublishReq(clientId, publishMessage.getVariableHead().getMessageId(), publishMessage).subscribe();
                    }
                    channel.writeAndFlush(publishMessage);
                }).then(storeManager.getSubscriptionService().subscribe(client)
                    .doOnNext(e -> {})
                      //  log.debug("Client 【{}】 subscribe topic 【{}】", clientId, topics))
                );
            })).subscribe();
    }
}