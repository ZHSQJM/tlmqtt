package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.store.service.PublishService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ConcurrentHashMap;

import static javax.print.attribute.standard.MediaSizeName.C;

/**
 * @Author: hszhou
 * @Date: 2025/4/18 14:46
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
public class DefaultPublishServiceImpl implements PublishService {


    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, PublishMessage>> PUBLISH_MAP = new ConcurrentHashMap<>();


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
