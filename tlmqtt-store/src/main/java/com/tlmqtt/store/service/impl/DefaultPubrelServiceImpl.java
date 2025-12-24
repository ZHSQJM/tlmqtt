package com.tlmqtt.store.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.PubrelService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultPubrelServiceImpl implements PubrelService {

    /**
     * Key: clientId
     * Value: Map<messageId, PubRelRequest>
     */
    private final Cache<String, Map<Long, TlMqttPubRelReq>> pubrelCache = Caffeine.newBuilder()
        // QoS 2 流程通常很快，如果 1 小时都没处理完，基本可以判定为客户端异常或链路中断
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(10000)
        .build();

    @Override
    public Mono<TlMqttPubRelReq> save(String clientId, Long messageId, TlMqttPubRelReq req) {
        return Mono.fromSupplier(() -> {
            Map<Long, TlMqttPubRelReq> messageMap = pubrelCache.get(clientId, k -> new ConcurrentHashMap<>());
            if (messageMap != null) {
                messageMap.put(messageId, req);
            }
            return req;
        });
    }

    @Override
    public Mono<TlMqttPubRelReq> clear(String clientId, Long messageId) {
        return Mono.fromSupplier(() -> {
            Map<Long, TlMqttPubRelReq> messageMap = pubrelCache.getIfPresent(clientId);
            if (messageMap != null) {
                return messageMap.remove(messageId);
            }
            return null;
        });
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(() -> {
            boolean existed = pubrelCache.getIfPresent(clientId) != null;
            pubrelCache.invalidate(clientId);
            return existed;
        });
    }

    @Override
    public Mono<TlMqttPubRelReq> find(String clientId, Long messageId) {
        return Mono.justOrEmpty(pubrelCache.getIfPresent(clientId))
            .mapNotNull(map -> map.get(messageId));
    }

    @Override
    public Flux<TlMqttPubRelReq> findAll(String clientId) {
        Map<Long, TlMqttPubRelReq> messageMap = pubrelCache.getIfPresent(clientId);
        if (messageMap == null || messageMap.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(messageMap.values());
    }

    /**
     * 实现观察者接口：当 Session 彻底清理时，释放该客户端所有挂起的 PUBREL 状态
     */
    @Override
    public Mono<Void> onSessionCleared(String clientId) {
        return Mono.fromRunnable(() -> {
            log.info("Observer: [PubrelService] cleaning data for clientId: [{}]", clientId);
            pubrelCache.invalidate(clientId);
        }).then();
    }
}
