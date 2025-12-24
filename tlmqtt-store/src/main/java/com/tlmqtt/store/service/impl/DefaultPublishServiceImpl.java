package com.tlmqtt.store.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.session.listener.SessionEventListener;
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
public class DefaultPublishServiceImpl implements PublishService {


    /**
     * 未确认消息缓存 (QoS 1/2)
     * Key: clientId
     * Value: Map<messageId, PublishRequest>
     */
    private final Cache<String, Map<Long, TlMqttPublishReq>> unackedCache = Caffeine.newBuilder()
                                                                                    // 离线消息最多保留1天
                                                                                    .expireAfterAccess(1, TimeUnit.DAYS)
                                                                                    // 最大保存1万个客户端的待确认消息
                                                                                    .maximumSize(10000)
                                                                                    .build();

    /**
     * 遗嘱消息缓存
     * Key: clientId
     * Value: PublishRequest
     */
    private final Cache<String, TlMqttPublishReq> willCache = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build();

    @Override
    public Mono<TlMqttPublishReq> save(String clientId, Long messageId, TlMqttPublishReq req) {
        return Mono.fromSupplier(() -> {
            // 获取该客户端的消息 Map，如果不存在则创建
            Map<Long, TlMqttPublishReq> messageMap = unackedCache.get(clientId, k -> new ConcurrentHashMap<>());
            if (messageMap != null) {
                messageMap.put(messageId, req);
            }
            return req;
        });
    }

    @Override
    public Mono<TlMqttPublishReq> clear(String clientId, Long messageId) {
        return Mono.fromSupplier(() -> {
            Map<Long, TlMqttPublishReq> messageMap = unackedCache.getIfPresent(clientId);
            if (messageMap != null) {
                return messageMap.remove(messageId);
            }
            return null;
        });
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(() -> {
            boolean existed = unackedCache.getIfPresent(clientId) != null;
            unackedCache.invalidate(clientId); // 直接从 Caffeine 中移除
            return existed;
        });
    }

    @Override
    public Mono<TlMqttPublishReq> find(String clientId, Long messageId) {
        return Mono.justOrEmpty(unackedCache.getIfPresent(clientId))
            .mapNotNull(map -> map.get(messageId));
    }

    @Override
    public Flux<TlMqttPublishReq> findAll(String clientId) {
        Map<Long, TlMqttPublishReq> messageMap = unackedCache.getIfPresent(clientId);
        if (messageMap == null || messageMap.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(messageMap.values());
    }

    @Override
    public Mono<Boolean> saveWill(String clientId, TlMqttPublishReq req) {
        return Mono.fromSupplier(() -> {
            willCache.put(clientId, req);
            return true;
        });
    }

    @Override
    public Mono<TlMqttPublishReq> findWill(String clientId) {
        return Mono.justOrEmpty(willCache.getIfPresent(clientId));
    }

    @Override
    public Mono<Boolean> clearWill(String clientId) {
        return Mono.fromSupplier(() -> {
            boolean existed = willCache.getIfPresent(clientId) != null;
            willCache.invalidate(clientId);
            return existed;
        });
    }

    /**
     * 响应式观察者回调：当 SessionService 清理会话时，自动触发此处清理
     */
    @Override
    public Mono<Void> onSessionCleared(String clientId) {
        return Mono.fromRunnable(() -> {
            log.info("Observer: [PublishService] cleaning data for clientId: [{}]", clientId);
            // 1. 清理未确认消息
            unackedCache.invalidate(clientId);
            // 2. 清理遗嘱消息
            willCache.invalidate(clientId);
        }).then();
    }
}
