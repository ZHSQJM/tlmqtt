package com.tlmqtt.store.service;

import com.tlmqtt.common.model.entity.PublishMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hszhou

 */
public interface PublishService {

    /**
     * 保存应该向某个客户端推送的消息
     * 当broker向订阅者推送消息的时候  对于qos1和qos2的消息 在发送之前 需要将这个消息保存起来
     *
     * @param clientId  订阅者client
     * @param messageId 消息id
     * @param req       消息
     * @return 是否保存成功
     */
    Mono<PublishMessage> save(String clientId, Long messageId, PublishMessage req);

    /**
     * 清除某个客户端的某个消息
     * 当订阅者收到broker的消息时，对于qos1和qos2的消息 在收到ack或者rec的消息后 需要清除消息
     *
     * @param clientId  订阅者的clientId
     * @param messageId 消息ID
     * @return 是否清除成功
     */
    Mono<PublishMessage> clear(String clientId, Long messageId);

    /**
     * 清除订阅者的所有消息 当订阅者断开连接的时候 需要清除所有消息
     *
     * @param clientId 订阅者id
     * @return 是否清除成功
     */
    Mono<Boolean> clearAll(String clientId);

    /**
     * 查找某个订阅者的某个消息
     *
     * @param clientId  订阅者的客户端ID
     * @param messageId 消息ID
     * @return 具体的消息
     */
    Mono<PublishMessage> find(String clientId, Long messageId);

    /**
     * 查找某个订阅者的所有消息
     *
     * @param clientId 订阅者的客户端ID
     * @return 所有的消息
     */
    Flux<PublishMessage> findAll(String clientId);


    /**
     * 保存客户端的遗嘱消息
     * @param clientId 客户端ID
     * @param req 消息体
     * @return 是否保存成功
     */
    Mono<Boolean> saveWill(String clientId, PublishMessage req);


    /**
     * 查询遗嘱消息
     * @param clientId 客户端ID
     * @return 消息体
     */
    Mono<PublishMessage> findWill(String clientId);


    /**
     * 清除某个客户端的遗嘱消息
     * @param clientId 客户端
     * @return  是否清除成功
     **/
    Mono<Boolean> clearWill(String clientId);
}
