package com.tlmqtt.common.config;

/**
 * 配置管理器单例，用于全局访问配置
 */
public class ConfigurationManager {
    
    private static volatile ConfigurationManager instance;
    private final MqttConfiguration configuration;
    
    private ConfigurationManager() {
        this.configuration = new MqttConfiguration();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    public MqttConfiguration getConfiguration() {
        return configuration;
    }
}