package com.tlmqtt.store.service.impl;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.ShareSubscribeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class DefaultShareSubscribeServiceImpl implements ShareSubscribeService {

    private static final Logger log = LoggerFactory.getLogger(DefaultShareSubscribeServiceImpl.class);

    /**主题 -> 该主题下的所有共享组名*/
    private final Cache<String, List<String>> TOPIC_GROUP = Caffeine.newBuilder().build();

    /**组名 -> 组内的成员列表*/
    private final Cache<String, List<TlSubClient>> GROUP_MEMBER = Caffeine.newBuilder().build();

    @Override
    public Mono<Boolean> subscribeShare(TlSubClient client) {
        return Mono.fromSupplier(() -> {
            String topic = client.getTopic();
            String group = client.getGroup();

            // 1. 维护主题与组的关系
            List<String> groups = TOPIC_GROUP.get(topic, k -> new CopyOnWriteArrayList<>());
            assert groups != null;
            if (!groups.contains(group)) {
                groups.add(group);
            }

            // 2. 维护组与成员的关系 (使用 CopyOnWriteArrayList 保证并发安全)
            List<TlSubClient> members = GROUP_MEMBER.get(group, k -> new CopyOnWriteArrayList<>());
            // 避免重复添加同一个客户端
            assert members != null;
            members.removeIf(m -> m.getClientId().equals(client.getClientId()));
            members.add(client);

            return true;
        });
    }

    @Override
    public Mono<Boolean> unsubscribeShare(TlSubClient client) {
        return Mono.fromSupplier(() -> {
            String group = client.getGroup();
            String topic = client.getTopic();

            // 1. 移除成员
            List<TlSubClient> members = GROUP_MEMBER.getIfPresent(group);
            if (members != null) {
                members.removeIf(m -> m.getClientId().equals(client.getClientId()));

                // 2. 如果组内没成员了，彻底销毁该组
                if (members.isEmpty()) {
                    GROUP_MEMBER.invalidate(group);

                    // 3. 同时从主题关联中移除该组名 (修复你的原逻辑漏洞)
                    List<String> groups = TOPIC_GROUP.getIfPresent(topic);
                    if (groups != null) {
                        groups.remove(group);
                        if (groups.isEmpty()) {
                            TOPIC_GROUP.invalidate(topic);
                        }
                    }
                }
            }
            return true;
        });
    }

    @Override
    public HashMap<String, List<TlSubClient>> getGroupMember(String topicName) {
        List<String> groupNames = TOPIC_GROUP.getIfPresent(topicName);
        if (groupNames == null || groupNames.isEmpty()) {
            return null;
        }

        HashMap<String, List<TlSubClient>> result = new HashMap<>(10);
        for (String groupName : groupNames) {
            List<TlSubClient> members = GROUP_MEMBER.getIfPresent(groupName);
            if (members != null && !members.isEmpty()) {
                result.put(groupName, members);
            }
        }
        return result.isEmpty() ? null : result;
    }


    /**
     * 实现观察者接口：Session销毁时自动清理该客户端在所有组中的订阅
     * @author zhouhs
     * @param: clientId
     * @return: reactor.core.publisher.Mono<java.lang.Void>
     **/

    @Override
    public Mono<Void> onSessionCleared(String clientId) {
        return Mono.fromRunnable(() -> {
            // 遍历所有组，移除该客户端
            GROUP_MEMBER.asMap().forEach((group, members) -> {
                members.removeIf(m -> m.getClientId().equals(clientId));// 如果移除后组空了，这里可以进一步清理，但为了性能通常建议在下次心跳或反注册时清理
                // 或者简单的全部反查一遍
            });
            log.debug("客户端【{}】清除订阅的共享订阅主题",clientId);
        }).then();
    }
}
