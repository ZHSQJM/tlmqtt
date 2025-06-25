package com.tlmqtt.store.service;

import com.tlmqtt.common.model.entity.PubrelMessage;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hszhou

 */
public interface PubrelService {

    /**
     * 保存订阅者的某个rel的消息
     * 当broker向订阅者推送rel的消息时，需要将这个消息保存起来防止没收到comp的消息时重发
     *
     * @param clientId  订阅者的客户端ID
     * @param messageId 消息ID
     * @param req       具体的消息
     * @return 是否保存成功
     */
    Mono<PubrelMessage> save(String clientId, Long messageId, PubrelMessage req);

    /**
     * 清除订阅者的某个rel的消息
     *
     * @param clientId  订阅者的客户端ID
     * @param messageId 消息
     * @return 是否保存成功
     */
    Mono<PubrelMessage> clear(String clientId, Long messageId);

    /**
     * 清除订阅者的所有rel的消息
     *
     * @param clientId 订阅者的Rel消息
     * @return 是否清除成功
     */
    Mono<Boolean> clearAll(String clientId);

    /**
     * 查找订阅者的某个rel消息
     *
     * @param clientId  订阅者的客户端ID
     * @param messageId 消息id
     * @return 具体的消息
     */
    Mono<PubrelMessage> find(String clientId, Long messageId);

    /**
     * 查找订阅者的所有rel消息
     *
     * @param clientId 客户端的ID
     * @return 消息列表
     */
    Flux<PubrelMessage> findAll(String clientId);

}
