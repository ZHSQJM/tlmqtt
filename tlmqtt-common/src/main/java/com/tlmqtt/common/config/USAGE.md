# 配置管理使用指南

## 概述

本项目提供了两种配置管理方式：
1. 传统的静态方法方式（TlConfig）- 即将废弃
2. 新的实例化方式（MqttConfiguration）- 推荐使用

## 推荐用法

### 1. 使用单例模式访问配置

```java
// 获取配置管理器实例
MqttConfiguration config = ConfigurationManager.getInstance().getConfiguration();

// 获取配置值
boolean retainAvailable = config.getBoolean(MqttConfiguration.RETAIN_AVAILABLE);
int maxPacketSize = config.getInt(MqttConfiguration.MAXIMUM_PACKET_SIZE);

// 设置配置值
config.setBoolean("CUSTOM_FEATURE_ENABLED", true);
config.setInt("CUSTOM_TIMEOUT", 5000);
```

### 2. 注入方式使用配置（适用于Spring等框架）

```java
@Component
public class MyService {
    
    private final MqttConfiguration configuration;
    
    public MyService(MqttConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public void doSomething() {
        boolean featureEnabled = configuration.getBoolean("FEATURE_ENABLED", false);
        // 使用配置...
    }
}
```

## 传统用法（不推荐）

```java
// 不推荐：使用已废弃的静态方法
boolean retainAvailable = TlConfig.getBoolean(TlConfig.RETAIN_AVAILABLE);
```

## 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| SESSION_EXPIRY_INTERVAL | int | 3600 | 会话最长过期时间(秒) |
| TOPIC_ALIAS_MAXIMUM | int | 200 | 服务器支持的最大主题别名数 |
| MAXIMUM_PACKET_SIZE | int | 65535 | 最大报文大小 |
| MAXIMUM_QOS | int | 2 | 最大支持的QoS等级 |
| RETAIN_AVAILABLE | boolean | true | 是否支持保留消息 |
| WILDCARD_SUBSCRIPTION_AVAILABLE | boolean | true | 是否支持通配符订阅 |
| SUBSCRIPTION_IDENTIFIERS_AVAILABLE | boolean | true | 是否支持订阅标识符 |
| SHARED_SUBSCRIPTION_AVAILABLE | boolean | true | 是否支持共享订阅 |
| REFUSE_CLIENTS | List<String> | empty | 拒绝连接的客户端列表 |
| INVALID_TOPIC_NAMES | List<String> | empty | 无效的主题名列表 |

## 迁移指南

从TlConfig迁移到MqttConfiguration非常简单：

**旧代码:**
```java
int qos = TlConfig.getInt(TlConfig.MAXIMUM_QOS);
boolean retain = TlConfig.getBoolean(TlConfig.RETAIN_AVAILABLE);
```

**新代码:**
```java
MqttConfiguration config = ConfigurationManager.getInstance().getConfiguration();
int qos = config.getInt(MqttConfiguration.MAXIMUM_QOS);
boolean retain = config.getBoolean(MqttConfiguration.RETAIN_AVAILABLE);
```