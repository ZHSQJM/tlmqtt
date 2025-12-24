package com.tlmqtt.store.service.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.session.listener.SessionEventListener;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultSessionServiceImpl implements SessionService {

    /**
     * 使用 Caffeine 替换 ConcurrentHashMap
     * expireAfterAccess: 实现 MQTT 的 KeepAlive/会话过期逻辑
     */

    // 1. Caffeine 仅作为高性能内存存储，不设置自动过期
    private final Cache<String, TlMqttSession> sessionCache = Caffeine.newBuilder()
        .maximumSize(100_000)
        .build();
    // 2. 引入时间轮或其他定时器处理“延时删除”
    private final Timer timer = new HashedWheelTimer();

    private final Map<String, Timeout> removalTasks = new ConcurrentHashMap<>();


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
            removalTasks.remove(clientId); // 确保任务也被移除
            return existed;
        }).flatMap(existed -> {
            if (existed) {
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
            if (session == null) return false;
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
            if (session == null) return false;
            session.getTopics().remove(subClient.getTopic());
            return true;
        });
    }

    @Override
    public Flux<TlMqttSession> findAll() {
        return Flux.fromIterable(sessionCache.asMap().values());
    }
    /**
     * 对应你原来的 cancelRemoveSession
     */
    @Override
    public Mono<Void> cancelRemoveSession(String clientId) {
        return Mono.fromRunnable(() -> {
            Timeout timeout = removalTasks.remove(clientId);
            if (timeout != null) {
                timeout.cancel();
                log.info("Client [{}] reconnected, removal task cancelled.", clientId);
            }
        });
    }

    @Override
    public Mono<Void> scheduleRemoval(String clientId, long expirySeconds) {
        return Mono.fromRunnable(() -> {
            // 如果 expirySeconds 为 0，应该立即清理 (由 Handler 判断或这里判断)
            if (expirySeconds <= 0) {
                this.clearAll(clientId).subscribe();
                return;
            }

            // 创建一个新的延时任务
            Timeout timeout = timer.newTimeout(t -> {
                log.info("Session Expiry Interval reached for [{}], clearing session...", clientId);
                removalTasks.remove(clientId);
                this.clearAll(clientId).subscribe();
            }, expirySeconds, TimeUnit.SECONDS);

            // 存入 Map，如果之前已有任务则取消旧的
            Timeout old = removalTasks.put(clientId, timeout);
            if (old != null) old.cancel();

            log.info("Scheduled session removal for [{}] after {}s", clientId, expirySeconds);
        });
    }
}
