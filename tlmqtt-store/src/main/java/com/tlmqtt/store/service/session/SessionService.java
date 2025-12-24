package com.tlmqtt.store.service.session;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.session.listener.SessionEventListener;
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
     * @return 保存结果
     */
    Mono<Boolean> save(TlMqttSession session);

    /**
     * 查询会话
     *
     * @param clientId 会话ID
     * @return 会话
     */
    Mono<TlMqttSession> find(String clientId);

    /**
     * 清理会话
     *
     * @param clientId 会话ID
     * @return 清理结果
     */
    Mono<Boolean> clear(String clientId);

    /**
     * 清除所有会话
     * 清理 Session 并通知观察者清理关联数据
     * @param clientId 会话ID
     * @return 清理结果
     */
    Mono<Boolean> clearAll(String clientId);

    /**
     * 添加订阅
     *
     * @param subClient 订阅
     * @return 添加结果
     */
    Mono<Boolean> addTopic(TlSubClient subClient);

    /**
     * 删除订阅
     *
     * @param subClient 订阅
     * @return 删除结果
     */
    Mono<Boolean> removeTopic(TlSubClient subClient);

    /**
     * 查询所有会话
     *
     * @return 所有会话
     */
    Flux<TlMqttSession> findAll();


    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    void addListener(SessionEventListener listener);


    /**
     * 取消删除会话
     * @param clientId 客户端
     * @return 删除结果
     */
    Mono<Void> cancelRemoveSession(String clientId);

    /**
     * 延迟删除会话
     *
     * @param clientId 客户端
     * @param expirySeconds 延迟时间
     * @return 延迟结果
     */
    Mono<Void> scheduleRemoval(String clientId, long expirySeconds);
}
