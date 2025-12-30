package com.tlmqtt.store.service.impl;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.RetainService;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author hszhou
 */
@Slf4j
public class DefaultRetainServiceImpl implements RetainService {



    /**
     * Key: Topic (String)
     * Value: TlMqttPublishReq
     */
    private final Cache<String, TlMqttPublishReq> retainCache = Caffeine.newBuilder()
        .maximumSize(50000)
        .build();

    @Override
    public Mono<Boolean> save(String topic, TlMqttPublishReq req) {
        return Mono.fromSupplier(() -> {
            // MQTT 规范：Payload 为空代表删除该主题的保留消息

            Object content = (req.getPayload() != null) ? req.getPayload().getContent() : null;

            if (content == null || (content instanceof String && "".equals(content))) {
                retainCache.invalidate(topic);
                log.debug("主题【{}】清除保留消息", topic);
            } else {
                // 存入缓存前，建议对 Req 进行深拷贝，防止原始 Req 被 Netty 释放后导致缓存失效
                // 这里假设你的 req.copy() 实现了深拷贝
                TlMqttPublishReq cacheReq = req.copy();
                // 增加引用计数防止内存被回收
                ReferenceCountUtil.retain(cacheReq);

                // 如果之前有旧消息，先释放旧消息的引用
                TlMqttPublishReq old = retainCache.getIfPresent(topic);
                if (old != null) {
                    ReferenceCountUtil.release(old);
                }

                retainCache.put(topic, cacheReq);
                log.debug("主题[{}]存储保留消息", topic);
            }
            return true;
        });
    }

    @Override
    public Flux<TlMqttPublishReq> find(String filter) {
        // 使用 Flux.defer 确保每次订阅都会执行扫描
        return Flux.defer(() -> {
            Stream<TlMqttPublishReq> stream;

            // 1. 判断是否包含通配符
            if (!filter.contains("+") && !filter.contains("#")) {
                // 精确匹配：直接从缓存取，性能最高
                TlMqttPublishReq exactMatch = retainCache.getIfPresent(filter);
                stream = (exactMatch != null) ? Stream.of(exactMatch) : Stream.empty();
            } else {
                // 2. 通配符匹配：扫描所有保留消息并过滤
                stream = retainCache.asMap().entrySet().stream()
                    .filter(entry -> matchesMqttTopic(filter, entry.getKey()))
                    .map(Map.Entry::getValue);
            }

            // 3. 返回副本，防止后续逻辑修改缓存中的原始对象
            return Flux.fromStream(stream.map(TlMqttPublishReq::copy));
        });
    }

    @Override
    public Mono<Boolean> clear(String topic) {
        return Mono.fromSupplier(() -> {
            TlMqttPublishReq old = retainCache.getIfPresent(topic);
            if (old != null) {
                retainCache.invalidate(topic);
                ReferenceCountUtil.release(old); // 释放引用计数
                return true;
            }
            return false;
        });
    }

    /**
     * 高效的主题匹配算法
     * @param filter 订阅的主题过滤器 (可能含 + #)
     * @param topic 存储的具体主题 (不含通配符)
     */
    private boolean matchesMqttTopic(String filter, String topic) {
        if (filter.equals(topic)) return true;

        String[] filterParts = filter.split("/");
        String[] topicParts = topic.split("/");

        for (int i = 0; i < filterParts.length; i++) {
            String f = filterParts[i];

            // # 匹配后续所有层级
            if (f.equals("#")) {
                return true;
            }

            // 如果 topic 层级已经用完，但 filter 还没到 #
            if (i >= topicParts.length) {
                return false;
            }

            // + 匹配单层，否则必须完全相等
            if (!f.equals("+") && !f.equals(topicParts[i])) {
                return false;
            }
        }

        // 如果 filter 走完了，需要判断 topic 是否也走完了
        return filterParts.length == topicParts.length;
    }

    @Override
    public Mono<Void> onSessionCleared(String clientId) {
        // 注意：Retain 消息是与主题绑定的，不应该随 Client 清理而清理
        // 这里的逻辑通常保持为空，除非你有特殊的业务需求
        return Mono.empty();
    }
}
