package com.tlmqtt.core.message;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: hszhou
 * @Date: 2025/6/10 15:33
 * @Description: 用于消息的转发等等
 */
@RequiredArgsConstructor
@Slf4j
public class TlMessageService {


    public static final int MAX_ID = 65535;

    private final AtomicLong messageId = new AtomicLong(0);

    @Getter
    private final TlStoreManager storeManager;
    @Getter
    private final ChannelManager channelManager;
    @Getter
    private final RetryManager retryManager;

    /**
     * @description: 用于转发消息给订阅topic的客户端
     * @author: hszhou
     * @datetime: 2025-05-08 13:48:43
     * @param: topic
     * @param: req
     **/
    public void publish(String topic, MqttQoS qoS, String content) {
        //找到所有的客户端
        storeManager.getSubscriptionService().find(topic).subscribe(client -> {
            log.debug("【Client】1. Find subscribe topic 【{}】 client 【{}】",topic,client);
            doPublish(topic, qoS, content, client);
        });
    }

    private void doPublish(String topic, MqttQoS qoS, String content, TlSubClient client) {
        TlMqttPublishReq message = ofMessage(topic, qoS, content, client);
        String clientId = client.getClientId();
        Long messageId = message.getVariableHead().getMessageId();
        log.debug("【Client】2. Find  client session 【{}】,message is 【{}】",client.getClientId(),message.getVariableHead().getMessageId());
        MqttQoS mqttQoS = message.getFixedHead().getQos();
        if (mqttQoS == MqttQoS.EXACTLY_ONCE || mqttQoS == MqttQoS.AT_LEAST_ONCE) {
            retryManager.retry(new TlRetryMessage(messageId, message), clientId, MqttMessageType.PUBLISH);
        }
            storeManager.getSessionService().find(clientId)
                .flatMap(session -> {
                    log.debug("Client session [{}] processing message [{}]", clientId, messageId);
                    return mqttQoS==MqttQoS.AT_MOST_ONCE
                        ?  Mono.just(message)
                        : storeManager.savePublishReq(clientId, messageId, message, mqttQoS);
                })
                .doOnError(e -> log.error("Publish failed for client [{}]", clientId, e))
                .subscribe(e -> {
                    log.debug("Client session [{}] processed message [{}]", clientId, messageId);
                    channelManager.writeAndFlush(clientId, message);
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

}
