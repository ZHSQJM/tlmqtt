package com.tlmqtt.common.config;

import java.util.List;

/**
 * 配置管理接口
 */
public interface Configuration {

    /**
     * 获取布尔型配置值
     * @param key 配置键
     * @return 配置值
     */
    boolean getBoolean(String key);

    /**
     * 获取布尔型配置值，带默认值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 获取整型配置值
     * @param key 配置键
     * @return 配置值
     */
    int getInt(String key);

    /**
     * 获取整型配置值，带默认值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getInt(String key, int defaultValue);

    /**
     * 获取字符串列表配置值
     * @param key 配置键
     * @return 配置值
     */
    List<String> getStringList(String key);

    /**
     * 设置布尔型配置值
     * @param key 配置键
     * @param value 配置值
     */
    void setBoolean(String key, boolean value);

    /**
     * 设置整型配置值
     * @param key 配置键
     * @param value 配置值
     */
    void setInt(String key, int value);

    /**
     * 设置字符串列表配置值
     * @param key 配置键
     * @param value 配置值
     */
    void setStringList(String key, List<String> value);

    /**
     * 更新字符串列表配置值（添加或删除）
     * @param key 配置键
     * @param value 要操作的值
     * @param operation 操作类型：true表示添加，false表示删除
     */
    void updateStringList(String key, String value, boolean operation);
}