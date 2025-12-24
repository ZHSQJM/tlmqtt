package com.tlmqtt.core.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class DefaultAliasServiceImpl implements AliasService {


    private static final Cache<String, ConcurrentHashMap<Integer, String>> CLIENT_ALIAS_MAP =
        Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((clientId, aliasMap, cause) -> {
                // 可选：记录日志或统计信息
                if (aliasMap != null) {

                }
            })
            .build();


    // 线程安全的put方法
    @Override
    public boolean put(String clientId, Integer alias, String topic) {
        if (clientId == null || alias == null || topic == null) {
            return false;
        }

        try {
            ConcurrentHashMap<Integer, String> aliasMap = CLIENT_ALIAS_MAP.get(
                clientId,
                id -> new ConcurrentHashMap<>(16, 0.75f, 4)
            );
            aliasMap.put(alias, topic);
            return true;
        } catch (Exception e) {
            // 记录日志
            return false;
        }
    }

    @Override
    public String get(String clientId, Integer alias) {
        Map<Integer, String> clientAliases = getClientAliases(clientId);
        return clientAliases.get(alias);
    }

    // 批量获取
    public Map<Integer, String> getClientAliases(String clientId) {
        ConcurrentHashMap<Integer, String> aliasMap = CLIENT_ALIAS_MAP.getIfPresent(clientId);
        return aliasMap != null ?
            Collections.unmodifiableMap(new HashMap<>(aliasMap)) :
            Collections.emptyMap();
    }

    // 统计信息
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, ConcurrentHashMap<Integer, String>> all = CLIENT_ALIAS_MAP.asMap();
        stats.put("clientCount", all.size());
        stats.put("totalAliases", all.values().stream()
            .mapToInt(Map::size)
            .sum());
        return stats;
    }
}
