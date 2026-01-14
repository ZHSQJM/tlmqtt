package com.tlmqtt.store.service.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.session.listener.SessionEventListener;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultSessionServiceImpl implements SessionService {

    /**
     * 使用 Caffeine 替换 ConcurrentHashMap
     * expireAfterAccess: 实现 MQTT 的 KeepAlive/会话过期逻辑
     */


    private final Cache<String, TlMqttSession> sessionCache = Caffeine.newBuilder()
        .maximumSize(10000000)
        .build();



    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(SessionEventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public Mono<Boolean> save(TlMqttSession session) {
        return Mono.fromSupplier(() -> {
            sessionCache.put(session.getClientId(), session);
            return true;
        });
    }

    @Override
    public Mono<TlMqttSession> find(String clientId) {
        // 仅仅是查询，不涉及逻辑
        return Mono.justOrEmpty(sessionCache.getIfPresent(clientId));
    }

    @Override
    public Mono<Boolean> clear(String clientId) {
        return Mono.fromSupplier(() -> {
            boolean existed = sessionCache.asMap().containsKey(clientId);
            sessionCache.invalidate(clientId);
            return existed;
        });
    }

    /**
     * 核心优化：清理所有数据并通知观察者
     */
    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(() -> {
            boolean existed = sessionCache.asMap().containsKey(clientId);
            sessionCache.invalidate(clientId);
            return existed;
        }).flatMap(existed -> {
            if (existed) {
                log.debug("【TLMQTT】Cleared all stored data for client: [{}]", clientId);
                return notifyObservers(clientId).thenReturn(true);
            }
            return Mono.just(false);
        });
    }

    /**
     * 通知所有观察者执行清理
     */
    private Mono<Void> notifyObservers(String clientId) {
        return Flux.fromIterable(listeners)
            .flatMap(listener -> listener.onSessionCleared(clientId))
            .then();
    }

    @Override
    public Mono<Boolean> addTopic(TlSubClient subClient) {
        return Mono.fromSupplier(() -> {
            TlMqttSession session = sessionCache.getIfPresent(subClient.getClientId());
            if (session == null) {
                return false;
            }
            session.getTopics().add(subClient.getTopic());
            // 重新 put 以确保某些实现下的缓存更新触发
            sessionCache.put(session.getClientId(), session);
            return true;
        });
    }

    @Override
    public Mono<Boolean> removeTopic(TlSubClient subClient) {
        return Mono.fromSupplier(() -> {
            TlMqttSession session = sessionCache.getIfPresent(subClient.getClientId());
            if (session == null) {
                return false;
            }
            session.getTopics().remove(subClient.getTopic());
            return true;
        });
    }

    @Override
    public Flux<TlMqttSession> findAll() {
        return Flux.fromIterable(sessionCache.asMap().values());
    }

}
