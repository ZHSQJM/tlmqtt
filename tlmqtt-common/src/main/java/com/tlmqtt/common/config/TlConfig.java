package com.tlmqtt.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 
 * @deprecated 请使用 {@link MqttConfiguration} 替代此类，此类将在未来版本中移除
 **/
public class TlConfig {

    /**定义MQTT的配置缓存*/
    private static final Cache<String, Object> CONFIG_CACHE = Caffeine.newBuilder().build();

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
    
    static {
        // 初始化配置缓存
        CONFIG_CACHE.invalidateAll();
        /*会话最长的过期时间*/
        CONFIG_CACHE.put(SESSION_EXPIRY_INTERVAL, SESSION_EXPIRY_INTERVAL_VALUE);
        /*服务器支持的最大别名的长度*/
        CONFIG_CACHE.put(TOPIC_ALIAS_MAXIMUM, TOPIC_ALIAS_MAXIMUM_VALUE);

        CONFIG_CACHE.put(MAXIMUM_PACKET_SIZE, MAXIMUM_PACKET_SIZE_VALUE);
        /*最大支持的qos等级*/
        CONFIG_CACHE.put(MAXIMUM_QOS, MAXIMUM_QOS_VALUE);
        /*保留可用*/
        CONFIG_CACHE.put(RETAIN_AVAILABLE, true);
        /*是否支持通配符订阅*/
        CONFIG_CACHE.put(WILDCARD_SUBSCRIPTION_AVAILABLE, true);
        /*订阅标识符可用*/
        CONFIG_CACHE.put(SUBSCRIPTION_IDENTIFIERS_AVAILABLE, true);
        /*是否支持共享订阅*/
        CONFIG_CACHE.put(SHARED_SUBSCRIPTION_AVAILABLE, true);
        /*拒绝的连接客户端*/
        CONFIG_CACHE.put(REFUSE_CLIENTS,Collections.emptyList());
        /*无效的主题名*/
        CONFIG_CACHE.put(INVALID_TOPIC_NAMES,Collections.emptyList());
    }

    /**
     * 获取配置 转换成boolean类型 如果为空就返回false
     * @param key 配置的key
     * @return boolean
     */
    public static boolean getBoolean(String key){
        Object value = CONFIG_CACHE.getIfPresent(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * 获取配置 转换成int类型 如果为空就返回0
     * @param key 配置的key
     * @return int
     */

    public static int getInt(String key){
        Object value = CONFIG_CACHE.getIfPresent(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 获取配置 转换成List<String>类型 如果为空就返回空的集合
     * @param key 配置的key
     * @return List<String>
     */
    public static List<String> getStringList(String key){
        Object value = CONFIG_CACHE.getIfPresent(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    /**
     * 更新配置 配置的key
     * @param key 配置的key
     * @param value 配置的value
     */
    public static void updateBoolean(String key, Boolean value){
        CONFIG_CACHE.put(key, value);
    }

    /**
     * 获取配置 转换成int类型 如果为空就返回0
     * @param key 配置的key
     * @param value 配置的value
     */
    public static void updateInt(String key, int value){
        CONFIG_CACHE.put(key, value);
    }

    /**
     * 更新配置  type是1 的话就移除缓存中的value，是2就新增value
     * @param key 配置的key
     * @param value 配置的value
     * @param type 配置的类型 1表示删除，2表示添加
     */
    public static void updateString(String key, String value, int type){
        // 新增value
        Object obj = CONFIG_CACHE.getIfPresent(key);
        if (obj instanceof List) {
            List<String> list = new CopyOnWriteArrayList<>((List<String>) obj);
            boolean b = type == 1 ? list.remove(value) : list.add(value);
            CONFIG_CACHE.put(key, list);
        }
    }
}