package com.tlmqtt.store.service;

import com.tlmqtt.common.model.entity.TlSubClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hszhou

 */
public interface SubscriptionService {


    /**
     * 保存客户订阅的主题
     * @param clientSub 客户端订阅主题对象
     * @return 客户端订阅主题对象
     */
    Mono<Boolean> subscribe(TlSubClient clientSub);


    /**
     * 解除订阅
     * @param clientId 客户端ID
     * @param topic 主题
     * @return 客户端订阅对象
     */
    Mono<Boolean> unsubscribe(String clientId, String  topic);

    /**
     * 获取订阅了 topic 的客户id
     * @param topic 主题
     * @return 所有定于主体的客户端对象
     */
    Flux<TlSubClient> find(String topic);

    /**
     *  清除客户端所有的订阅
     * @param clientId 订阅者的客户端ID
     * @return 是否清除成功
     */
    Mono<Boolean> clear(String clientId);


}
