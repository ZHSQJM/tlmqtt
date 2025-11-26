package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.request.TlMqttPublishReq;
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
    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, TlMqttPublishReq>> PUBLISH_MAP = new ConcurrentHashMap<>();


    /**
     * key:clientId
     * value:PublishMessage
     */
    public static final ConcurrentHashMap<String,TlMqttPublishReq> WILL_MAP = new ConcurrentHashMap<>();

    @Override
    public Mono<TlMqttPublishReq> save(String clientId, Long messageId, TlMqttPublishReq req) {

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
    public Mono<TlMqttPublishReq> clear(String clientId, Long messageId) {
        return Mono.fromSupplier(()-> PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16))
                  .remove(String.valueOf(messageId)));
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return Mono.fromSupplier(()-> PUBLISH_MAP.remove(clientId)==null);
    }

    @Override
    public Mono<TlMqttPublishReq> find(String clientId, Long messageId) {
        return Mono.fromSupplier(()->PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(10)).get(String.valueOf(messageId)));
    }

    @Override
    public Flux<TlMqttPublishReq> findAll(String clientId) {
      return Flux.fromIterable(  PUBLISH_MAP.getOrDefault(clientId, new ConcurrentHashMap<>(16)).values());
    }

    @Override
    public Mono<Boolean> saveWill(String clientId, TlMqttPublishReq req) {
      //  log.info("开始保存 will 消息 - clientId: {}, req: {}", clientId, req);
        return Mono.fromCallable(() -> {
            TlMqttPublishReq existing = WILL_MAP.putIfAbsent(clientId, req);
        //    log.debug("保存结果 - clientId: {}, 是否新增: {}", clientId, existing == null);
            return existing == null;
        });
    }

    @Override
    public Mono<TlMqttPublishReq> findWill(String clientId) {
       // log.info("查询 will 消息 - clientId: {}", clientId);
        TlMqttPublishReq req = WILL_MAP.get(clientId);
        //log.debug("当前 WILL_MAP 内容: {}", WILL_MAP);
        //log.info("查询结果 - clientId: {}, 存在: {}", clientId, req != null);
        return Mono.justOrEmpty(req);
    }

    @Override
    public Mono<Boolean> clearWill(String clientId) {

       // log.info("移除遗嘱消息");
        TlMqttPublishReq req = WILL_MAP.get(clientId);
       // log.debug("1当前 WILL_MAP 内容: {}", WILL_MAP);
      //  log.info("1查询结果 - clientId: {}, 存在: {}", clientId, req != null);
        return Mono.just(WILL_MAP.remove(clientId) != null);
    }
}
