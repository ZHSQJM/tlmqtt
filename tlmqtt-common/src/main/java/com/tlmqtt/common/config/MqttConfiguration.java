package com.tlmqtt.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQTT配置管理实现类
 */
public class MqttConfiguration implements Configuration {

    /** 定义MQTT的配置缓存 */
    private final Cache<String, Object> configCache = Caffeine.newBuilder().build();

    public static final String SESSION_EXPIRY_INTERVAL = "SESSION_EXPIRY_INTERVAL";
    public static final int SESSION_EXPIRY_INTERVAL_VALUE = 3600;
    public static final String TOPIC_ALIAS_MAXIMUM = "TOPIC_ALIAS_MAXIMUM";
    public static final int TOPIC_ALIAS_MAXIMUM_VALUE = 200;
    public static final String MAXIMUM_PACKET_SIZE = "MAXIMUM_PACKET_SIZE";
    public static final int MAXIMUM_PACKET_SIZE_VALUE = 65535;
    public static final String MAXIMUM_QOS = "MAXIMUM_QOS";
    public static final int MAXIMUM_QOS_VALUE = 2;
    public static final String RETAIN_AVAILABLE = "RETAIN_AVAILABLE";
    public static final String WILDCARD_SUBSCRIPTION_AVAILABLE = "WILDCARD_SUBSCRIPTION_AVAILABLE";
    public static final String SUBSCRIPTION_IDENTIFIERS_AVAILABLE = "SUBSCRIPTION_IDENTIFIERS_AVAILABLE";
    public static final String SHARED_SUBSCRIPTION_AVAILABLE = "SHARED_SUBSCRIPTION_AVAILABLE";
    public static final String REFUSE_CLIENTS = "REFUSE_CLIENTS";
    public static final String INVALID_TOPIC_NAMES = "INVALID_TOPIC_NAMES";

    /**
     * 构造函数，初始化默认配置
     */
    public MqttConfiguration() {
        initializeDefaultConfig();
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfig() {
        // 初始化配置缓存
        configCache.invalidateAll();
        /* 会话最长的过期时间 */
        configCache.put(SESSION_EXPIRY_INTERVAL, SESSION_EXPIRY_INTERVAL_VALUE);
        /* 服务器支持的最大别名的长度 */
        configCache.put(TOPIC_ALIAS_MAXIMUM, TOPIC_ALIAS_MAXIMUM_VALUE);

        configCache.put(MAXIMUM_PACKET_SIZE, MAXIMUM_PACKET_SIZE_VALUE);
        /* 最大支持的qos等级 */
        configCache.put(MAXIMUM_QOS, MAXIMUM_QOS_VALUE);
        /* 保留可用 */
        configCache.put(RETAIN_AVAILABLE, true);
        /* 是否支持通配符订阅 */
        configCache.put(WILDCARD_SUBSCRIPTION_AVAILABLE, true);
        /* 订阅标识符可用 */
        configCache.put(SUBSCRIPTION_IDENTIFIERS_AVAILABLE, true);
        /* 是否支持共享订阅 */
        configCache.put(SHARED_SUBSCRIPTION_AVAILABLE, true);
        /* 拒绝的连接客户端 */
        configCache.put(REFUSE_CLIENTS, Collections.emptyList());
        /* 无效的主题名 */
        configCache.put(INVALID_TOPIC_NAMES, Collections.emptyList());
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = configCache.getIfPresent(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    @Override
    public int getInt(String key) {
        return getInt(key, 0);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Object value = configCache.getIfPresent(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    @Override
    public List<String> getStringList(String key) {
        Object value = configCache.getIfPresent(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    @Override
    public void setBoolean(String key, boolean value) {
        configCache.put(key, value);
    }

    @Override
    public void setInt(String key, int value) {
        configCache.put(key, value);
    }

    @Override
    public void setStringList(String key, List<String> value) {
        configCache.put(key, value);
    }

    @Override
    public void updateStringList(String key, String value, boolean operation) {
        // 新增value
        Object obj = configCache.getIfPresent(key);
        if (obj instanceof List) {
            List<String> list = new CopyOnWriteArrayList<>((List<String>) obj);
            if (operation) {
                // 添加元素
                list.add(value);
            } else {
                // 删除元素
                list.remove(value);
            }
            configCache.put(key, list);
        }
    }
}