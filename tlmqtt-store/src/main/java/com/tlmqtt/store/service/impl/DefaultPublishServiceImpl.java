package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.store.service.PublishService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author hszhou
 */
@Slf4j
public class DefaultPublishServiceImpl implements PublishService {


    /**
     * key:clientId
     * value:messageId:PublishMessage
     */
    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, PublishMessage>> PUBLISH_MAP = new ConcurrentHashMap<>();


    /**
     * key:clientId
     * value:PublishMessage
     */
    public static final ConcurrentHashMap<String,PublishMessage> WILL_MAP = new ConcurrentHashMap<>();

    @Override
    public Mono<PublishMessage> save(String clientId, Long messageId, PublishMessage req) {

        return Mono.fromSupplier(()->
                PUBLISH_MAP.compute(clientId, (k, v) -> {
                    if (v == null) {
                        v = new ConcurrentHashMap<>(16);
                    }
                    v.computeIfAbsent(String.valueOf(messageId), key -> req);
                    return v;
                })).thenReturn(req);
            //.subscribeOn(Schedulers.boundedElastic());

    }

    @Override
    public Mono<PublishMessage> clear(String clientId, Long messageId) {
        return Mono.fromSupplier(()-> PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16))
                  .remove(String.valueOf(messageId)));
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(()-> PUBLISH_MAP.remove(clientId)==null);
    }

    @Override
    public Mono<PublishMessage> find(String clientId, Long messageId) {
        return Mono.fromSupplier(()->PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(10)).get(String.valueOf(messageId)));
    }

    @Override
    public Flux<PublishMessage> findAll(String clientId) {
      return Flux.fromIterable(  PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16)).values());
    }

    @Override
    public Mono<Boolean> saveWill(String clientId, PublishMessage req) {
       return Mono.defer(()-> WILL_MAP.putIfAbsent(clientId, req) == null ? Mono.just(true) : Mono.just(false));
    }

    @Override
    public Mono<PublishMessage> findWill(String clientId) {
        return Mono.defer(()->Mono.justOrEmpty(WILL_MAP.get(clientId)));
    }

    @Override
    public Mono<Boolean> clearWill(String clientId) {
        return Mono.defer(()->Mono.just(WILL_MAP.remove(clientId) != null));
    }
}
