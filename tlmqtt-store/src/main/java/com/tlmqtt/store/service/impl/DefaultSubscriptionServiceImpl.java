package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.topic.TlTopicTrie;
import com.tlmqtt.store.service.SessionService;
import com.tlmqtt.store.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @Author: hszhou
 * @Date: 2025/1/8 9:24
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
public class DefaultSubscriptionServiceImpl implements SubscriptionService {


    private final TlTopicTrie trie ;

    private final SessionService sessionService;

    public DefaultSubscriptionServiceImpl(SessionService sessionService){
        this.sessionService = sessionService;
        this.trie = new TlTopicTrie();
    }
    @Override
    public Mono<Boolean> subscribe(TlSubClient clientSub) {
        return Mono.fromCallable(()->{
            trie.insert(clientSub.getTopic(),clientSub);
            return clientSub;
        }).flatMap(sessionService::addTopic)
          .thenReturn(true)
          .onErrorResume(ex->{
              log.error("clientId = 【{}】 订阅失败",clientSub.getClientId(),ex);
              return Mono.just(false);
          });

    }

    @Override
    public Mono<Boolean> unsubscribe(String clientId, String topic) {

        TlSubClient subClient = createSubClient(clientId, topic);
        return Mono.fromRunnable(()->trie.remove(topic,subClient))
                    .then(sessionService.removeTopic(subClient))
                    .thenReturn(true);
    }

    @Override
    public Flux<TlSubClient> find(String topic) {
        return Flux.defer(() -> Flux.fromIterable(trie.search(topic)));
    }

    @Override
    public Mono<Boolean> clear(String clientId) {
           //清除客户端的所有订阅
            return sessionService.find(clientId)
            .flatMap(session -> Flux.fromIterable(session.getTopics())
                 .flatMap(topic -> unsubscribe(clientId, topic))
                 .then(Mono.fromRunnable(()-> trie.removeAll(clientId)))
                 .then(sessionService.clear(clientId))
                 .thenReturn(true))
                 .defaultIfEmpty(false)
                 .onErrorResume(ex->Mono.just(false));

    }

    private TlSubClient createSubClient(String clientId, String topic) {
        TlSubClient subClient = new TlSubClient();
        subClient.setClientId(clientId);
        subClient.setTopic(topic);
        return subClient;
    }
}
