package com.tlmqtt.core;

import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.channel.ChannelManager;
import com.tlmqtt.core.retry.RetryService;
import com.tlmqtt.core.retry.TlRetryMessage;
import com.tlmqtt.store.service.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: hszhou
 * @Date: 2025/5/8 13:45
 * @Description: 转发消息给客户端
 */
@Slf4j
@Setter
@Getter
public class TlStoreManager {

    private SessionService sessionService;

    private SubscriptionService subscriptionService;

    private ChannelManager channelManager;

    private RetryService retryService;

    private PublishService publishService;

    private PubrelService pubrelService;

    private RetainService retainService;


    public TlStoreManager(SessionService sessionService, SubscriptionService subscriptionService,
        ChannelManager channelManager, RetryService retryService, PublishService publishService,
        PubrelService pubrelService,RetainService retainService) {
        this.sessionService = sessionService;
        this.subscriptionService = subscriptionService;
        this.channelManager = channelManager;
        this.retryService = retryService;
        this.publishService = publishService;
        this.pubrelService = pubrelService;
        this.retainService = retainService;
    }

    public static final int MAX_ID = 65535;

    private final AtomicLong messageId = new AtomicLong(0);

    /**
     * @description: 用于转发消息给订阅topic的客户端
     * @author: hszhou
     * @datetime: 2025-05-08 13:48:43
     * @param:
     * @param: topic
     * @param: req
     * @return: void
     **/
    public void publish(String topic, MqttQoS qoS, String content) {
        //找到所有的客户端
        subscriptionService.find(topic).parallel().subscribe(client -> {
           log.info("find subscribe topic 【{}】 client 【{}】",topic,client);
            doPublish(topic, qoS, content, client);
        });
    }

    private void doPublish(String topic, MqttQoS qoS, String content, TlSubClient client) {
        TlMqttPublishReq message = ofMessage(topic, qoS, content, client);

        sessionService.find(client.getClientId()).flatMap(session -> {
            channelManager.writeAndFlush(client.getClientId(), message);
            log.debug("find  client session 【{}】,message is 【{}】",client.getClientId(),message);
            MqttQoS mqttQoS = message.getFixedHead().getQos();
            if (mqttQoS == MqttQoS.EXACTLY_ONCE || mqttQoS == MqttQoS.AT_LEAST_ONCE) {
                Long messageId = message.getVariableHead().getMessageId();
                savePublishReq(client.getClientId(), messageId, message,mqttQoS);
            }
            return Mono.empty();
        }).subscribe();

    }

    private void savePublishReq(String clientId, Long messageId, TlMqttPublishReq message,MqttQoS qoS) {
        PublishMessage publishMessage = new PublishMessage();
        publishMessage.setClientId(clientId);
        publishMessage.setTopic(message.getVariableHead().getTopic());
        publishMessage.setQos(qoS.value());
        publishMessage.setMessageId(message.getVariableHead().getMessageId());
        publishMessage.setMessage(message.getPayload().getContent().toString());
        publishMessage.setDup(true);
        publishMessage.setRetain(false);
        log.info("save publish messageId is【{}】", messageId);
        publishService.save(clientId, messageId, publishMessage).subscribe(e -> {
            TlRetryMessage retryMessage = new TlRetryMessage();
            retryMessage.setMessageId(messageId);
            retryMessage.setMessage(message);
            retryService.retry(retryMessage, clientId);
        });
    }

    /**
     * 构建一个新的消息
     *
     * @param topic 主题
     * @param qoS qos
     * @param content 内容
     * @param client 客户端
     * @return TlMqttPublishReq
     * @author hszhou
     * @datetime: 2025-05-16 09:52:45
     **/
    public TlMqttPublishReq ofMessage(String topic, MqttQoS qoS, String content, TlSubClient client) {
        int subQos = client.getQos();
        int realQos = Math.min(subQos, qoS.value());
        MqttQoS mqttQoS = MqttQoS.valueOf(realQos);
        if (mqttQoS == MqttQoS.AT_MOST_ONCE) {
            return TlMqttPublishReq.build(topic, mqttQoS, false, content, 0L);
        }
        Long messageId = getMessageId();
        return TlMqttPublishReq.build(topic, mqttQoS, false, content, messageId);
    }

    public Long getMessageId() {
        long andIncrement = messageId.getAndIncrement();
        if (andIncrement > MAX_ID) {
            messageId.set(0);
            return messageId.getAndIncrement();
        }
        return andIncrement;
    }

    /**
     * 清除客户端所有的信息
     * @author hszhou
     * @datetime: 2025-05-26 18:45:57
     * @param clientId 客户端
     * @return Mono<Void>
     **/
    public Mono<Void> clearAll(String clientId) {
      return sessionService.find(clientId).flatMap(
            e -> Mono.when(
                subscriptionService.clear(clientId),
                publishService.clearAll(clientId),
                pubrelService.clearAll(clientId),
                publishService.clearWill(clientId)));
    }
}
