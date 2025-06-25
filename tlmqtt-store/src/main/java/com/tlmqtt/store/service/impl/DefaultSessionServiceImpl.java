package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultSessionServiceImpl implements SessionService {

    /**
     * 对应的是客户端的ID后面是客户端对应的session
     */
    private static final ConcurrentHashMap<String, TlMqttSession> CLIENT_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> save(TlMqttSession session) {
        return Mono.fromSupplier(() -> CLIENT_SESSIONS.put(session.getClientId(), session)).thenReturn(true);
    }

    @Override
    public Mono<TlMqttSession> find(String clientId) {
        return Mono.justOrEmpty(CLIENT_SESSIONS.get(clientId))
            .doOnError(e -> log.error("session: save clientId【{}】 failed",clientId, e))
            .doOnSuccess(e->log.debug("session: save clientId 【{}】 session status【{}】",clientId,e));
    }

    @Override
    public Mono<Boolean> clear(String clientId) {
        return Mono.fromSupplier(() -> CLIENT_SESSIONS.remove(clientId) != null).defaultIfEmpty(false)
            .doOnError(e -> log.error("session: save clientId【{}】 failed",clientId, e))
            .doOnSuccess(e->log.debug("session: save clientId 【{}】 session status【{}】",clientId,e));
    }

    @Override
    public Mono<Boolean> addTopic(TlSubClient subClient) {
        return Mono.fromSupplier(() -> {
            TlMqttSession session = CLIENT_SESSIONS.get(subClient.getClientId());
            if (session == null) {
                return false;
            }
            session.getTopics().add(subClient.getTopic());
            return true;
        });
    }

    @Override
    public Mono<Boolean> removeTopic(TlSubClient subClient) {
        return Mono.fromSupplier(() -> {
            TlMqttSession session = CLIENT_SESSIONS.get(subClient.getClientId());
            if (session == null) {
                return false;
            }
            session.getTopics().remove(subClient.getTopic());
            return true;
        });
    }

    @Override
    public Flux<TlMqttSession> findAll() {
        return Flux.fromIterable(CLIENT_SESSIONS.values());
    }
}
