package com.tlmqtt.core.alias;


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
            })
            .build();

    /**
     * 添加别名
     * @param clientId 客户端
     * @param alias 别名
     * @param topic 主题
     */
    @Override
    public void put(String clientId, Integer alias, String topic) {
        if (clientId == null || alias == null || topic == null) {
            return;
        }

        try {
            ConcurrentHashMap<Integer, String> aliasMap = CLIENT_ALIAS_MAP.get(
                clientId,
                id -> new ConcurrentHashMap<>(16, 0.75f, 4)
            );
            assert aliasMap != null;
            aliasMap.put(alias, topic);
        } catch (Exception e) {
            // 记录日志
        }
    }

    @Override
    public String get(String clientId, Integer alias) {
        Map<Integer, String> clientAliases = getClientAliases(clientId);
        return clientAliases.get(alias);
    }


    /**
     * 获取客户端的别名
     * @param clientId 客户端
     * @return 别名
     */
    public Map<Integer, String> getClientAliases(String clientId) {
        ConcurrentHashMap<Integer, String> aliasMap = CLIENT_ALIAS_MAP.getIfPresent(clientId);
        return aliasMap != null ?
            Collections.unmodifiableMap(new HashMap<>(aliasMap)) :
            Collections.emptyMap();
    }

}
