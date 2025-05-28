package com.tlmqtt.boot.store;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @Author: hszhou
 * @Date: 2025/1/8 9:11
 * @Description: 基于redis的会话服务实现
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class RedisSessionServiceImpl implements SessionService {


   private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private static final String PREFIX = "session:";

    @Override
    public Mono<Boolean> save(TlMqttSession session) {
        String clientId = session.getClientId();
        return reactiveRedisTemplate.opsForValue()
              .set(PREFIX + clientId, session)
              .doOnError(e -> log.error("session: save clientId【{}】 failed",clientId, e))
              .doOnSuccess(e->log.debug("session: save clientId 【{}】 session status【{}】",clientId,e));
    }

    @Override
    public Mono<TlMqttSession> find(String clientId) {
        return reactiveRedisTemplate.opsForValue().get(PREFIX + clientId)
            .cast(TlMqttSession.class)
            .doOnError(e -> log.error("session: find clientId【{}】 session failed",clientId, e))
            .doOnSuccess(e->log.debug("session: find clientId 【{}】 session【{}】",clientId,e));

    }

    @Override
    public Mono<Boolean> clear(String clientId) {
        return reactiveRedisTemplate.opsForValue().delete(PREFIX + clientId)
            .doOnError(e -> log.error("session: clear clientId【{}】 session failed",clientId, e))
            .doOnSuccess(e->log.debug("session: clear clientId 【{}】 session【{}】",clientId,e));
    }

    @Override
    public Mono<Boolean> addTopic(TlSubClient subClient) {
       return find(subClient.getClientId())
            .flatMap(session -> {
                session.getTopics().add(subClient.getTopic());
                return save(session);
            });

    }

    @Override
    public Mono<Boolean> removeTopic(TlSubClient subClient) {
        return find(subClient.getClientId())
             .flatMap(session -> {
                session.getTopics().remove(subClient.getTopic());
                return save(session);
            });
    }

    @Override
    public Flux<TlMqttSession> findAll() {
          return reactiveRedisTemplate.keys(PREFIX + "*").cast(TlMqttSession.class);
    }


}
