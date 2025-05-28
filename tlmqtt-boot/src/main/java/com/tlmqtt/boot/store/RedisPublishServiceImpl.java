package com.tlmqtt.boot.store;

import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @Author: hszhou
 * @Date: 2025/2/19 16:54
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublishServiceImpl implements PublishService {

    private final static String WILL_KEY = "will:";

    private final static String PUBLISH_KEY = "publish:";

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Override
    public Mono<PublishMessage> save(String clientId, Long messageId, PublishMessage req) {
        return reactiveRedisTemplate.opsForHash().put(PUBLISH_KEY + clientId, String.valueOf(messageId), req)
            .flatMap(value -> {
                log.info("保存成功 - Key: {}, 值: {}, value{}", PUBLISH_KEY+clientId, req,value);
                return find(clientId, messageId);
            })
            .doOnError(e -> log.error("publish: save clientId【{}】-messageId 【{}】 failed", clientId,messageId, e))
            .doOnSuccess(e -> log.debug("publish: save clientId 【{}】-messageId 【{}】 status【{}】", clientId,messageId, e));
    }

    @Override
    public Mono<PublishMessage> clear(String clientId, Long messageId) {

        return find(clientId,messageId)
            .flatMap(e -> reactiveRedisTemplate.opsForHash().remove(PUBLISH_KEY + clientId, String.valueOf(messageId)).thenReturn(e))
            .doOnError(e -> log.error("publish: clear clientId【{}】-messageId 【{}】 failed", clientId,messageId, e))
            .doOnSuccess(e -> log.debug("publish: clear clientId 【{}】-messageId 【{}】 status【{}】", clientId,messageId, e));
    }

    @Override
    public Mono<Boolean> clearAll(String clientId) {
        return reactiveRedisTemplate.opsForHash().delete(PUBLISH_KEY + clientId)
            .doOnError(e -> log.error("publish: clearAll clientId【{}】failed", clientId, e))
            .doOnSuccess(e -> log.debug("publish: clearAll clientId 【{}】 status【{}】", clientId, e));
    }

    @Override
    public Mono<PublishMessage> find(String clientId, Long messageId) {
        return reactiveRedisTemplate.opsForHash().get(PUBLISH_KEY + clientId, String.valueOf(messageId))
            .cast(PublishMessage.class)
            .doOnError(e -> log.error("publish: find clientId【{}】-messageId 【{}】 failed", clientId,messageId, e))
            .doOnSuccess(e -> log.debug("publish: find clientId 【{}】-messageId 【{}】 status【{}】", clientId,messageId, e));
    }

    @Override
    public Flux<PublishMessage> findAll(String clientId) {
        return reactiveRedisTemplate.opsForHash().keys(PUBLISH_KEY + clientId)
            .cast(PublishMessage.class)
            .doOnError(e -> log.error("publish: findAll clientId【{}】 failed", clientId, e))
            .doOnComplete(() -> log.debug("publish: findAll clientId 【{}】", clientId));

    }

    @Override
    public Mono<Boolean> saveWill(String clientId, PublishMessage req) {
        return reactiveRedisTemplate.opsForValue().set(WILL_KEY + clientId, req)
            .doOnError(e -> log.error("will: save clientId【{}】 failed", clientId, e))
            .doOnSuccess(e -> log.debug("will: save clientId 【{}】 status【{}】", clientId, e));
    }

    @Override
    public Mono<PublishMessage> findWill(String clientId) {
        return reactiveRedisTemplate.opsForValue().get(WILL_KEY + clientId)
            .cast(PublishMessage.class)
            .doOnError(e -> log.error("will: find clientId【{}】failed", clientId, e))
            .doOnSuccess(e -> log.debug("will: find clientId 【{}】status【{}】", clientId, e));
    }

    @Override
    public Mono<Boolean> clearWill(String clientId) {
        return reactiveRedisTemplate.opsForValue().delete(WILL_KEY + clientId)
            .doOnError(e -> log.error("will: clear clientId【{}】failed", clientId, e))
            .doOnSuccess(e -> log.debug("will: clear clientId 【{}】 status【{}】", clientId, e));
    }

}
