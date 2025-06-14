package com.tlmqtt.core.manager;

import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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

    private PublishService publishService;

    private PubrelService pubrelService;

    private RetainService retainService;


    public TlStoreManager(SessionService sessionService, SubscriptionService subscriptionService, PublishService publishService,
        PubrelService pubrelService,RetainService retainService) {
        this.sessionService = sessionService;
        this.subscriptionService = subscriptionService;
        this.publishService = publishService;
        this.pubrelService = pubrelService;
        this.retainService = retainService;
    }


    /**
     * 保存publish消息
     * @author hszhou
     * @datetime: 2025-06-10 15:40:02
     * @param clientId 客户端ID
     * @param messageId 消息ID
     * @param message 消息体
     * @param qoS qos
     * @return Mono<PublishMessage>
     **/
    public Mono<PublishMessage> savePublishReq(String clientId, Long messageId, TlMqttPublishReq message,MqttQoS qoS) {
        PublishMessage publishMessage = new PublishMessage();
        publishMessage.setClientId(clientId);
        publishMessage.setTopic(message.getVariableHead().getTopic());
        publishMessage.setQos(qoS.value());
        publishMessage.setMessageId(message.getVariableHead().getMessageId());
        publishMessage.setMessage(message.getPayload().getContent().toString());
        publishMessage.setDup(true);
        publishMessage.setRetain(false);
        log.debug("save publish messageId is【{}】", messageId);
        return publishService.save(clientId, messageId, publishMessage);
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
