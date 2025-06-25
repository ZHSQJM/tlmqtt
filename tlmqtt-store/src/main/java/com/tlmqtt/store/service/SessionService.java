package com.tlmqtt.store.service;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hszhou
 */
public interface SessionService {

    /**
     * 保存会话
     *
     * @param session 会话
     * @return Boolean 会话
     */
    Mono<Boolean> save(TlMqttSession session);

    /**
     * 通过 clientId 获取会话
     * @param clientId 客户端id
     * @return TlMqttSession 会话
     */
    Mono<TlMqttSession> find(String  clientId);

    /**
     * 清理会话
     *
     * @param clientId 客户端
     * @return Boolean 是否清除成功
     */
    Mono<Boolean> clear(String clientId);


    /**
     * session添加主题
     * @author hszhou
     *  2025-05-22 08:43:18
     * @param subClient 主题
     * @return Boolean 是否添加成功
     **/
    Mono<Boolean> addTopic(TlSubClient subClient);


    /**
     * session移除主题
     * @author hszhou
     * 2025-05-22 08:44:02
     * @param subClient 客户端订阅
     * @return Boolean 是否移除成功
     **/
    Mono<Boolean> removeTopic(TlSubClient subClient);

    /**
     * 返回所有的客户端雷彪
     *
     * @return TlMqttSession 客户端列表
     */
    Flux<TlMqttSession> findAll();



}
