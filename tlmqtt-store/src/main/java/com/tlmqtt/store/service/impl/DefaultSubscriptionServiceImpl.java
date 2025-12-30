package com.tlmqtt.store.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.topic.TlTopicTrie;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultSubscriptionServiceImpl implements SubscriptionService {


    private final TlTopicTrie trie;
    private final SessionService sessionService;

    /**反向索引：记录每个客户端订阅了哪些主题，用于快速清理  Key: clientId, Value: Set of Topics*/
    private final Cache<String, Set<String>> clientSubscriptionCache = Caffeine.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    public DefaultSubscriptionServiceImpl(SessionService sessionService) {
        this.sessionService = sessionService;
        this.trie = new TlTopicTrie();
    }

    @Override
    public Mono<Boolean> subscribe(TlSubClient clientSub) {
        return Mono.fromCallable(() -> {
                // 1. 插入 Trie 树
                trie.insert(clientSub.getTopic(), clientSub);
                // 2. 更新反向索引
                Set<String> topics = clientSubscriptionCache.get(clientSub.getClientId(), k -> ConcurrentHashMap.newKeySet());
                topics.add(clientSub.getTopic());
                return clientSub;
            })
            .flatMap(sessionService::addTopic)
            .thenReturn(true);
    }

    @Override
    public Mono<Boolean> unsubscribe(String clientId, String topic) {
        return Mono.fromRunnable(() -> {
                TlSubClient subClient = new TlSubClient();
                subClient.setClientId(clientId);
                subClient.setTopic(topic);

                // 1. 从 Trie 树移除
                trie.remove(topic, subClient);
                // 2. 从反向索引移除
                Set<String> topics = clientSubscriptionCache.getIfPresent(clientId);
                if (topics != null) {
                    topics.remove(topic);
                }
            })
            .then(sessionService.removeTopic(createSubClient(clientId, topic)))
            .thenReturn(true);
    }

    @Override
    public Flux<TlSubClient> find(String topic) {
        return Flux.defer(() -> Flux.fromIterable(trie.search(topic)))
            .distinct(TlSubClient::getClientId);
    }

    /**
     * 实现观察者接口：当 Session 清理时被调用
     */
    @Override
    public Mono<Void> onSessionCleared(String clientId) {
        return Mono.fromRunnable(() -> {
            //log.info("Observer:[SubscriptionService] cleaning data for clientId[{}]", clientId);
            Set<String> topics = clientSubscriptionCache.getIfPresent(clientId);
            if (topics != null) {
                // 利用反向索引进行精准删除，而不是全树扫描
                topics.forEach(topic -> {
                    TlSubClient subClient = new TlSubClient();
                    subClient.setClientId(clientId);
                    trie.remove(topic, subClient);
                });
                clientSubscriptionCache.invalidate(clientId);
                log.debug("清除订阅客户端【{}】订阅的主题",clientId);
            }
        }).then();
    }

    @Override
    public Mono<Boolean> clear(String clientId) {
        // 现在只需触发一次精准清理即可
        return onSessionCleared(clientId).thenReturn(true);
    }

    private TlSubClient createSubClient(String clientId, String topic) {
        TlSubClient subClient = new TlSubClient();
        subClient.setClientId(clientId);
        subClient.setTopic(topic);
        return subClient;
    }
}
