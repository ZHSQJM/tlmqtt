<h1 align="center">TL-MQTT</h1>

<p align="center">
  <a href="https://github.com/ZHSQJM/tlmqtt/blob/main/LICENSE">
    <img alt="apache" src="https://img.shields.io/badge/license-Apache%202-blue"/>
  </a>
  <a href="https://netty.io/">
    <img alt="netty" src="https://img.shields.io/badge/netty-4.2.8.Final-green"/>
  </a>
  <a href="https://projectreactor.io/">
    <img alt="project-reactor" src="https://img.shields.io/badge/reactor-3.4.34-yellow"/>
  </a>
  <a href="https://github.com/ben-manes/caffeine">
    <img alt="caffeine" src="https://img.shields.io/badge/caffeine-3.2.0-orange"/>
  </a>
  <a href="https://mqtt.org/">
    <img alt="mqtt-3.1.1" src="https://img.shields.io/badge/mqtt-3.1.1-green"/>
    <img alt="mqtt-5.0" src="https://img.shields.io/badge/mqtt-5.0-blue"/>
  </a>
  <a href="https://github.com/ZHSQJM/tlmqtt/releases">
    <img alt="version" src="https://img.shields.io/badge/version-1.2.0-brightgreen"/>
  </a>
</p>

<p align="center">
  <b>轻量级、高性能、可嵌入的 MQTT Broker</b><br/>
  基于 Java 开发，快速将任何 Spring Boot 应用转变为 MQTT 服务器
</p>

---

## 📖 项目简介

**TL-MQTT** 是一款基于 Java 开发的轻量级、高性能嵌入式 MQTT Broker，专为物联网应用设计。它可以快速集成到任何 Spring Boot 应用中，让您的应用秒变 MQTT 服务器。

### 核心特性

- ⚡ **高性能**: 基于 Netty 异步网络框架，支持数万级并发连接
- 🪶 **轻量级**: 可嵌入式部署，无需独立安装 MQTT Broker
- 🔌 **易集成**: 提供 Spring Boot Starter，一个注解即可启用
- 🌐 **协议完整**: 完整支持 MQTT 3.1.1 和 MQTT 5.0 协议
- 🔒 **安全可靠**: 支持 SSL/TLS 加密、多种认证方式和 ACL 权限控制
- 🔧 **高度可扩展**: 提供丰富的接口，支持自定义存储、认证、授权等

---

## 🚀 快速开始

### 环境要求

- **JDK**: 1.8 或更高版本
- **Spring Boot**: 2.7.4 或更高版本
- **Maven**: 3.6+ 或 Gradle 6.0+

### 1. 添加依赖

在您的 Spring Boot 项目中添加 Maven 依赖：

```xml
<dependency>
    <groupId>io.github.zhsqjm</groupId>
    <artifactId>tlmqtt-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

### 2. 启用 MQTT 服务

在 Spring Boot 启动类上添加 `@EnableTlMqtt` 注解：

```java
@SpringBootApplication
@EnableTlMqtt
public class MqttApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqttApplication.class, args);
    }
}
```

### 3. 配置文件（可选）

在 `application.yml` 中添加配置：

```yaml
tlmqtt:
  port:
    mqtt: 1883              # MQTT 端口
    websocket: 8083         # WebSocket 端口
  auth:
    enabled: true           # 启用认证
    user:
      - username: admin
        password: admin123
        id: 1
  ssl:
    enabled: false          # SSL/TLS 加密
```

### 4. 启动应用

启动 Spring Boot 应用后，MQTT 服务器将自动运行在配置的端口上。

### 5. 测试连接

使用 MQTT 客户端工具（如 MQTTX、Mosquitto）连接测试：

```bash
# 使用 mosquitto_pub 发布消息
mosquitto_pub -h localhost -p 1883 -t test/topic -m "Hello TL-MQTT" -u admin -P admin123

# 使用 mosquitto_sub 订阅消息
mosquitto_sub -h localhost -p 1883 -t test/topic -u admin -P admin123
```

---

## 📚 核心功能

### 协议支持

| 协议 | 版本 | 支持状态 |
|------|------|---------|
| MQTT | 3.1.1 | ✅ 完整支持 |
| MQTT | 5.0 | ✅ 完整支持 |
| WebSocket | - | ✅ 支持 |
| SSL/TLS | 1.2/1.3 | ✅ 支持单向和双向认证 |

### QoS 服务质量

- **QoS 0**: 最多一次传递（At most once）
- **QoS 1**: 至少一次传递（At least once）
- **QoS 2**: 恰好一次传递（Exactly once）

### MQTT 功能

| 功能 | 说明 | 支持状态 |
|------|------|---------|
| 会话持久化 | 断线重连后恢复会话状态 | ✅ |
| 保留消息 | 新订阅者接收最后一条保留消息 | ✅ |
| 遗嘱消息 | 客户端异常断开时发送遗嘱 | ✅ |
| 共享订阅 | 多个订阅者负载均衡接收消息 | ✅ |
| 主题通配符 | 支持 `+` 和 `#` 通配符 | ✅ |
| 主题别名 | MQTT 5.0 主题别名功能 | ✅ |
| 自定义拦截器 | 消息发布/订阅拦截 | ✅ |

### 认证与授权

#### 认证方式

1. **固定用户认证**: 基于配置文件的用户名密码认证
2. **HTTP 认证**: 通过 HTTP 接口进行认证
3. **数据库认证**: 基于 MySQL 数据库的认证
4. **自定义认证**: 实现 `AuthenticationManager` 接口

#### 授权（ACL）

基于文件的细粒度权限控制，支持：
- 按用户名控制
- 按客户端 ID 控制
- 按 IP 地址控制
- 支持主题通配符
- 支持发布/订阅权限分离

---

## 🔧 配置详解

### 完整配置示例

```yaml
tlmqtt:
  # 线程配置
  bossThreadSize: 8         # Boss 线程数
  workThreadSize: 16        # Worker 线程数

  # 会话配置
  session:
    timeout: 60             # 会话超时时间（秒）
    delay: 5                # ACK 消息重发延迟（秒）
    maxRetry: 3             # QoS 消息最大重试次数

  # 端口配置
  port:
    mqtt: 1883              # MQTT 端口
    sslMqtt: 8883           # MQTT SSL 端口
    websocket: 8083         # WebSocket 端口
    sslWebsocket: 8084      # WebSocket SSL 端口

  # SSL/TLS 配置
  ssl:
    enabled: false          # 是否启用 SSL
    certPath: /path/to/server.crt      # 证书路径
    privatePath: /path/to/server.key   # 私钥路径

  # 认证配置
  auth:
    enabled: true           # 是否启用认证
    user:                   # 固定用户列表
      - username: admin
        password: admin123
        id: 1
      - username: device01
        password: device123
        id: 2

  # 通道配置
  channel:
    writeLimit: 104857600   # 出站带宽限制（字节/秒）
    readLimit: 52428800     # 入站带宽限制（字节/秒）
    checkInterval: 1000     # 流量统计周期（毫秒）
    maxTime: 20971520       # 最大突发流量（字节）
    lowWaterMark: 131072    # 低水位标记
    highWaterMark: 524288   # 高水位标记

  # 业务线程池配置
  business:
    corePoolSize: 16        # 核心线程数
    maxPoolSize: 32         # 最大线程数
    queueCapacity: 10000    # 队列容量
    keepAliveSeconds: 60    # 非核心线程存活时间
```

---

## 🏗️ 架构设计

### 模块结构

```
tl-mqtt
├── tlmqtt-common              # 公共模块：模型、枚举、异常、工具类
├── tlmqtt-core                # 核心模块：MQTT 协议处理
├── tlmqtt-store               # 存储模块：数据持久化接口和实现
├── tlmqtt-authentication      # 认证模块：多种认证方式
├── tlmqtt-authorization       # 授权模块：ACL 权限控制
├── tlmqtt-bootstrap           # 启动模块：服务器初始化
├── tlmqtt-source              # 数据源模块：Kafka、MySQL、InfluxDB 等
├── tlmqtt-rule                # 规则引擎模块
├── tlmqtt-spring-boot-starter # Spring Boot 自动配置
├── tlmqtt-dashboard           # 管理面板（开发中）
└── tlmqtt-demo                # 示例应用
```

### 技术栈

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 网络框架 | Netty | 4.2.8.Final | 高性能异步网络通信 |
| 响应式编程 | Project Reactor | 3.4.34 | 响应式流处理 |
| 应用框架 | Spring Boot | 2.7.14+ | 应用开发框架 |
| 缓存 | Caffeine | 3.2.0 | 高性能本地缓存 |
| 数据库连接池 | HikariCP | 4.0.3 | 数据库连接管理 |
| 消息队列 | Kafka | 3.9.1 | 消息流转发 |
| 时序数据库 | InfluxDB | 6.10.0 | 时序数据存储 |
| 高性能队列 | Disruptor | 3.4.2 | 无锁并发队列 |
| 工具库 | Hutool | 5.8.24 | Java 工具集 |

### 核心流程

```
客户端连接
    ↓
SSL/TLS 握手（可选）
    ↓
MQTT CONNECT 消息
    ↓
认证验证
    ↓
会话管理
    ↓
消息处理（发布/订阅）
    ↓
ACL 权限检查
    ↓
消息路由和分发
    ↓
持久化存储（可选）
```

---

## 🔐 安全配置

### SSL/TLS 配置

#### 1. 生成自签名证书（测试用）

```bash
# 生成私钥
openssl genrsa -out server.key 2048

# 生成证书签名请求
openssl req -new -key server.key -out server.csr

# 生成自签名证书
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
```

#### 2. 配置 SSL

```yaml
tlmqtt:
  ssl:
    enabled: true
    certPath: /path/to/server.crt
    privatePath: /path/to/server.key
  port:
    sslMqtt: 8883
```

### ACL 权限控制

在 `resources/acl.conf` 中配置权限规则：

```plaintext
# 格式：类型:值 | 资源类型:资源 | 操作 | 权限

# 禁止用户 admin 订阅 system/# 主题
user:admin | topic:system/# | sub | deny

# 允许用户 device01 发布到 sensor/# 主题
user:device01 | topic:sensor/# | pub | allow

# 禁止客户端 ID 为 test 的客户端订阅任何主题
client:test | topic:* | sub | deny

# 禁止 IP 192.168.1.100 发布消息
ip:192.168.1.100 | topic:* | pub | deny

# 默认规则：允许所有操作
user:* | topic:* | * | allow
```

---

## 🔌 扩展开发

### 自定义数据持久化

实现相应的服务接口来自定义数据存储方式：

```java
@Service
public class CustomSessionService implements SessionService {

    @Override
    public void saveSession(String clientId, Session session) {
        // 自定义会话存储逻辑（如 Redis、MongoDB）
    }

    @Override
    public Session getSession(String clientId) {
        // 自定义会话读取逻辑
        return null;
    }

    @Override
    public void removeSession(String clientId) {
        // 自定义会话删除逻辑
    }
}
```

### 自定义认证

```java
@Component
public class CustomAuthenticationManager implements AuthenticationManager {

    @Override
    public boolean authenticate(String username, String password) {
        // 自定义认证逻辑（如 LDAP、OAuth2）
        return true;
    }
}
```

### 消息拦截器

```java
@Component
public class CustomMessageInterceptor implements MessageInterceptor {

    @Override
    public boolean beforePublish(String clientId, String topic, byte[] payload) {
        // 发布前拦截
        return true;
    }

    @Override
    public void afterPublish(String clientId, String topic, byte[] payload) {
        // 发布后处理
    }
}
```

---

## 📊 数据源集成

TL-MQTT 支持将 MQTT 消息转发到多种数据源：

### Kafka 集成

```yaml
tlmqtt:
  source:
    kafka:
      enabled: true
      bootstrapServers: localhost:9092
      topic: mqtt-messages
```

### MySQL 集成

```yaml
tlmqtt:
  source:
    mysql:
      enabled: true
      url: jdbc:mysql://localhost:3306/mqtt
      username: root
      password: password
```

### InfluxDB 集成

```yaml
tlmqtt:
  source:
    influxdb:
      enabled: true
      url: http://localhost:8086
      token: your-token
      org: your-org
      bucket: mqtt-data
```

---

## 🧪 测试

### 使用 MQTTX 客户端

1. 下载 [MQTTX](https://mqttx.app/)
2. 创建新连接：
   - Host: `localhost`
   - Port: `1883`
   - Username: `admin`
   - Password: `admin123`
3. 订阅主题：`test/#`
4. 发布消息到：`test/hello`

### 使用 Mosquitto 命令行

```bash
# 订阅
mosquitto_sub -h localhost -p 1883 -t "test/#" -u admin -P admin123 -v

# 发布
mosquitto_pub -h localhost -p 1883 -t "test/hello" -m "Hello World" -u admin -P admin123
```

### 使用 Java 客户端

```java
MqttClient client = new MqttClient("tcp://localhost:1883", "client-id");
MqttConnectOptions options = new MqttConnectOptions();
options.setUserName("admin");
options.setPassword("admin123".toCharArray());

client.connect(options);
client.subscribe("test/#", (topic, message) -> {
    System.out.println("Received: " + new String(message.getPayload()));
});

client.publish("test/hello", "Hello TL-MQTT".getBytes(), 1, false);
```

---

## 📈 性能优化

### 推荐配置

#### 高并发场景（10000+ 连接）

```yaml
tlmqtt:
  bossThreadSize: 16
  workThreadSize: 32
  channel:
    lowWaterMark: 262144    # 256KB
    highWaterMark: 1048576  # 1MB
  business:
    corePoolSize: 32
    maxPoolSize: 64
    queueCapacity: 20000
```

#### 高吞吐场景（大量消息）

```yaml
tlmqtt:
  channel:
    writeLimit: 209715200   # 200MB/s
    readLimit: 104857600    # 100MB/s
  business:
    corePoolSize: 64
    maxPoolSize: 128
```

### JVM 参数建议

```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -jar your-app.jar
```

---

## 🐛 故障排查

### 常见问题

#### 1. 连接被拒绝

**原因**: 认证失败或端口未开放

**解决**:
- 检查用户名密码是否正确
- 确认防火墙是否开放端口
- 查看日志：`logging.level.com.tlmqtt: debug`

#### 2. 消息丢失

**原因**: QoS 设置不当或会话未持久化

**解决**:
- 使用 QoS 1 或 QoS 2
- 启用会话持久化
- 检查客户端 cleanSession 设置

#### 3. 性能问题

**原因**: 线程池配置不当或内存不足

**解决**:
- 调整线程池参数
- 增加 JVM 堆内存
- 启用 G1GC 垃圾回收器

---

## 🗺️ 路线图

### 已完成 ✅

- [x] MQTT 3.1.1 协议支持
- [x] MQTT 5.0 协议支持
- [x] WebSocket 支持
- [x] SSL/TLS 加密
- [x] 多种认证方式
- [x] ACL 权限控制
- [x] 共享订阅
- [x] 数据源集成

### 开发中 🚧

- [ ] Web 管理面板
- [ ] 集群支持
- [ ] 监控指标（Prometheus）
- [ ] 系统主题（$SYS）

### 计划中 📋

- [ ] MQTT 桥接
- [ ] 规则引擎增强
- [ ] Docker 镜像
- [ ] Kubernetes 部署支持
- [ ] 性能基准测试报告

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

### 如何贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Java 编码规范
- 添加必要的注释
- 编写单元测试
- 更新相关文档

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 🙏 致谢

感谢以下开源项目的启发和帮助：

- [MqttWk](https://github.com/Wizzercn/MqttWk)
- [SMQTTX](https://github.com/quickmsg/smqttx)
- [Netty](https://netty.io/)
- [Project Reactor](https://projectreactor.io/)

---

## 📞 联系方式

- **GitHub**: https://github.com/ZHSQJM/tlmqtt
- **Issues**: https://github.com/ZHSQJM/tlmqtt/issues
- **Email**: 2534798858@qq.com

---

<p align="center">
  <b>如果这个项目对您有帮助，请给我们一个 ⭐ Star！</b><br/>
  您的支持是我们持续改进的最大动力！
</p>

<p align="center">
  Made with ❤️ by TL-MQTT Team
</p>
