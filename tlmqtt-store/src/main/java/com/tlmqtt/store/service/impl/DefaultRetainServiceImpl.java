package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.topic.TlTopicTrie;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.SubscriptionService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hszhou
 * @Date: 2025/1/8 14:17
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class DefaultRetainServiceImpl implements RetainService {




    public static final ConcurrentHashMap<String, PublishMessage> RETAIN_MAP = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> save(String topic, PublishMessage req) {
        return Mono.fromSupplier(() -> { RETAIN_MAP.put(topic, req);
            return true;
        });
    }


    @Override
    public Mono<PublishMessage> find(String topic) {
        return Mono.fromSupplier(() -> RETAIN_MAP.entrySet().stream()
            .filter(entry -> matchesMqttTopic(topic, entry.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(null));

    }

    @Override
    public Mono<Boolean> clear(String topic) {
       return Mono.fromSupplier(()-> RETAIN_MAP.remove(topic)==null);
    }

    /**
     * 主题匹配逻辑
     * @param filter
     * @param topic
     * @return
     */
    private boolean matchesMqttTopic(String filter, String topic) {
        String[] filterParts = filter.split("/");
        String[] topicParts = topic.split("/");

        for (int i = 0; i < filterParts.length; i++) {
            String filterPart = filterParts[i];
            if (filterPart.equals("#")) {
                return i == filterParts.length - 1; // # 必须是最后一级
            }
            if (i >= topicParts.length) {
                return false;
            }
            String topicPart = topicParts[i];
            if (!filterPart.equals("+") && !filterPart.equals(topicPart)) {
                return false;
            }
        }
        return filterParts.length == topicParts.length;
    }
}
