package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.PubrelService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hszhou
 */
public class DefaultPubrelServiceImpl implements PubrelService {

    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, TlMqttPubRelReq>> PUBREL_MAP = new ConcurrentHashMap<>();

    @Override
    public Mono<TlMqttPubRelReq> save(String clientId, Long messageId, TlMqttPubRelReq req) {


        return Mono.fromSupplier(()->
            PUBREL_MAP.compute(clientId, (k, v) -> {
                if (v == null) {
                    v = new ConcurrentHashMap<>(16);
                }
                v.computeIfAbsent(String.valueOf(messageId), key -> req);
                return v;
            })).thenReturn(req);
    }

    @Override
    public Mono<TlMqttPubRelReq> clear(String clientId, Long messageId) {
        return Mono.fromSupplier(()->  PUBREL_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16))
                .remove(String.valueOf(messageId)));
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(()->  PUBREL_MAP.remove(clientId)).thenReturn(true);
    }

    @Override
    public Mono<TlMqttPubRelReq> find(String clientId, Long messageId) {

        return Mono.fromSupplier(()->PUBREL_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16)).get(String.valueOf(messageId)));
    }

    @Override
    public Flux<TlMqttPubRelReq> findAll(String clientId) {
        return Flux.fromIterable(  PUBREL_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16)).values());
    }
}
