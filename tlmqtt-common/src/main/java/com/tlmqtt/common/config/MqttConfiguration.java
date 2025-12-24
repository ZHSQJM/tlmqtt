package com.tlmqtt.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * MQTT配置管理实现类
 * @author hszhou
 */
public class MqttConfiguration  {

    private final Cache<String, Integer> INT_CACHE = Caffeine.newBuilder().build();
    private final Cache<String, Boolean> BOOLEAN_CACHE = Caffeine.newBuilder().build();
    private final Cache<String, List<String>> LIST_CACHE = Caffeine.newBuilder().build();

    @Getter
    public enum Property {
        /**
         * 最大包大小
         */
        MAXIMUM_PACKET_SIZE("MAXIMUM_PACKET_SIZE", 65535),
        SESSION_EXPIRY_INTERVAL("SESSION_EXPIRY_INTERVAL", 3600),
        TOPIC_ALIAS_MAXIMUM("TOPIC_ALIAS_MAXIMUM", 200),
        MAXIMUM_QOS("MAXIMUM_QOS", 2),
        RETAIN_AVAILABLE("RETAIN_AVAILABLE", true),
        WILDCARD_SUBSCRIPTION_AVAILABLE("WILDCARD_SUBSCRIPTION_AVAILABLE", true),
        SUBSCRIPTION_IDENTIFIERS_AVAILABLE("SUBSCRIPTION_IDENTIFIERS_AVAILABLE", true),
        SHARED_SUBSCRIPTION_AVAILABLE("SHARED_SUBSCRIPTION_AVAILABLE", true),
        REFUSE_CLIENTS("REFUSE_CLIENTS", Collections.emptyList()),
        INVALID_TOPIC_NAMES("INVALID_TOPIC_NAMES", Collections.emptyList());

        private final String key;
        private final Object defaultValue;

        Property(String key, Object defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    public static final String SESSION_EXPIRY_INTERVAL = "SESSION_EXPIRY_INTERVAL";
    public static final String TOPIC_ALIAS_MAXIMUM = "TOPIC_ALIAS_MAXIMUM";
    public static final String MAXIMUM_PACKET_SIZE = "MAXIMUM_PACKET_SIZE";
    public static final String MAXIMUM_QOS = "MAXIMUM_QOS";
    public static final String RETAIN_AVAILABLE = "RETAIN_AVAILABLE";
    public static final String WILDCARD_SUBSCRIPTION_AVAILABLE = "WILDCARD_SUBSCRIPTION_AVAILABLE";
    public static final String SUBSCRIPTION_IDENTIFIERS_AVAILABLE = "SUBSCRIPTION_IDENTIFIERS_AVAILABLE";
    public static final String SHARED_SUBSCRIPTION_AVAILABLE = "SHARED_SUBSCRIPTION_AVAILABLE";
    public static final String REFUSE_CLIENTS = "REFUSE_CLIENTS";
    public static final String INVALID_TOPIC_NAMES = "INVALID_TOPIC_NAMES";

    public static final Integer SESSION_EXPIRY_INTERVAL_VALUE = 3600;
    public static final Integer TOPIC_ALIAS_MAXIMUM_VALUE = 200;
    public static final Integer MAXIMUM_PACKET_SIZE_VALUE = 65535;
    public static final int MAXIMUM_QOS_VALUE = 2;

    private final List<Consumer<Property>> configListeners = new CopyOnWriteArrayList<>();

    public MqttConfiguration() {
        initializeDefaultConfig();
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfig() {
        // 清理缓存
        INT_CACHE.invalidateAll();
        BOOLEAN_CACHE.invalidateAll();
        LIST_CACHE.invalidateAll();

        /* 数值型配置 */
        INT_CACHE.put(SESSION_EXPIRY_INTERVAL, SESSION_EXPIRY_INTERVAL_VALUE);
        INT_CACHE.put(TOPIC_ALIAS_MAXIMUM, TOPIC_ALIAS_MAXIMUM_VALUE);
        INT_CACHE.put(MAXIMUM_PACKET_SIZE, MAXIMUM_PACKET_SIZE_VALUE);
        INT_CACHE.put(MAXIMUM_QOS, MAXIMUM_QOS_VALUE);

        /* 布尔型配置 */
        BOOLEAN_CACHE.put(RETAIN_AVAILABLE, true);
        BOOLEAN_CACHE.put(WILDCARD_SUBSCRIPTION_AVAILABLE, true);
        BOOLEAN_CACHE.put(SUBSCRIPTION_IDENTIFIERS_AVAILABLE, true);
        BOOLEAN_CACHE.put(SHARED_SUBSCRIPTION_AVAILABLE, true);

        /* 列表型配置 */
        LIST_CACHE.put(REFUSE_CLIENTS, new CopyOnWriteArrayList<>());
        LIST_CACHE.put(INVALID_TOPIC_NAMES, new CopyOnWriteArrayList<>());
    }
    // --- Getter ---

    public Boolean getBoolean(String key) {
        return BOOLEAN_CACHE.get(key, k -> false);
    }

    public Integer getInt(String key) {
        return INT_CACHE.get(key, k -> 0);
    }

    public List<String> getList(String key) {
        return LIST_CACHE.get(key, k -> Collections.emptyList());
    }

    // --- Setter & Notify ---

    public void setInt(String key, int value) {
        INT_CACHE.put(key, value);
        notifyListeners(key);
    }

    public void setBoolean(String key, boolean value) {
        BOOLEAN_CACHE.put(key, value);
        notifyListeners(key);
    }

    public void setList(String key, String value, boolean isAdd) {
        List<String> list = LIST_CACHE.get(key, k -> new CopyOnWriteArrayList<>());
        if (isAdd) {
            list.add(value);
        } else {
            list.remove(value);
        }
        LIST_CACHE.put(key, list);
        notifyListeners(key);
    }

    private void notifyListeners(String key) {
        for (Property p : Property.values()) {
            if (p.getKey().equals(key)) {
                configListeners.forEach(l -> l.accept(p));
                break;
            }
        }
    }

    public void addListener(Consumer<Property> listener) {
        this.configListeners.add(listener);
    }


    public ConcurrentMap<String,Integer> getIntAll(){
        return INT_CACHE.asMap();
    }

    public ConcurrentMap<String,Boolean> getBooleanAll(){
        return BOOLEAN_CACHE.asMap();
    }

    public ConcurrentMap<String,List<String>> getListAll(){
        return LIST_CACHE.asMap();
    }
}