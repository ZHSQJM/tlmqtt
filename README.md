<h1 align="center">TLMQTT</h1>
<p align="center">
  <a href="https://github.com/quickmsg/smqttx/blob/release/ignite/README.md">
    <img alt="license" src="https://img.shields.io/badge/license-Apache%202-blue"/>
  </a>
  <img alt="netty" src="https://img.shields.io/badge/netty-4.1.8.Final-green"/>
  <img alt="reactor" src="https://img.shields.io/badge/projectreactor-3.4.34-yellow"/>
  <img alt="mqtt" src="https://img.shields.io/badge/mqtt-3.1.1%20%2F%205.0-blue"/>
</p>

`tlmqtt` 是一款基于 Java 开发的轻量级、高性能嵌入式 MQTT Broker。它以一个 Spring Boot Starter 的形式提供，只需引入依赖、加一个注解，就能把任意 Spring Boot 应用快速搭建成 MQTT 服务。

---

## 目录

- [核心功能](#核心功能)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [完整配置参考](#完整配置参考)
- [模块与依赖](#模块与依赖)
- [认证 Authentication](#认证-authentication)
- [授权 ACL](#授权-acl)
- [WebSocket 与 SSL](#websocket-与-ssl)
- [QoS 消息流程](#qos-消息流程)
- [主题通配符](#主题通配符)
- [共享订阅](#共享订阅)
- [扩展点](#扩展点)
- [感谢](#感谢)

---

## 核心功能

### 协议支持
- **标准 MQTT 协议**：完整支持 MQTT 3.1.1 与 MQTT 5.0
- **WebSocket 协议**：支持基于 WebSocket 的 MQTT 通信（路径 `/mqtt`）
- **SSL/TLS**：支持 TLS 加密（单向 / 双向）

### 服务质量（QoS）
- **QoS 0**：至多一次（无需确认）
- **QoS 1**：至少一次（PUBLISH / PUBACK，未确认自动重发）
- **QoS 2**：恰好一次（PUBLISH / PUBREC / PUBREL / PUBCOMP 完整握手）

### 功能特性
- **会话保持**：断线后会话仍保留，重连时恢复订阅状态与未确认消息
- **保留消息（Retain）** 与 **遗嘱消息（Will）**
- **主题通配符**：`+` / `#` 匹配
- **共享订阅**：`$share/group/topic`、`$queue/topic`
- **流量控制**：基于 In-Flight 窗口 + 高/低水位线 + 带宽整形
- **认证**：本地配置（FIXED）/ HTTP / SQL（MySQL），可同时启用，任一通过即可
- **授权**：基于文件（acl.conf）的发布/订阅权限控制
- **自定义拦截器**：`PublishInterceptor`
- **可扩展持久化**：会话 / 消息 / 订阅 / 保留消息等存储接口均可自定义

---

## 环境要求

- **JDK**：>= 1.8
- **Spring Boot**：>= 2.7.4

---

## 快速开始

### 1. 引入依赖

在 `pom.xml` 中加入 Starter（核心能力，开箱即用）：

```xml
<dependency>
    <groupId>io.github.zhsqjm</groupId>
    <artifactId>tlmqtt-spring-boot-starter</artifactId>
    <version>1.2.0.1</version>
</dependency>
```

> 认证与授权是按需加载的独立模块，需要时再单独引入，见 [模块与依赖](#模块与依赖)。

### 2. 配置文件（可选）

在 `application.yml` 中添加配置。**所有配置都在 `tlmqtt` 前缀下**，不配置则使用默认值：

```yaml
tlmqtt:
  port:
    mqtt: 1883        # 原生 MQTT 端口
    websocket: 8083   # WebSocket 端口
  session:
    timeout: 60       # 会话超时时间（秒）
    delay: 10         # ACK 未确认后的重发延迟（秒）
    maxRetry: 3       # QoS 1/2 消息最大重试次数
```

### 3. 应用启动

在 Spring Boot 启动类上添加 `@EnableTlMqtt`：

```java
@SpringBootApplication
@EnableTlMqtt
public class MqttApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqttApplication.class, args);
    }
}
```

启动成功后，即可用 MQTT 客户端连接 `tcp://localhost:1883` 进行通信。

---

## 完整配置参考

所有字段及其默认值（前缀统一为 `tlmqtt`）：

```yaml
tlmqtt:
  # ===== 端口 =====
  port:
    mqtt: 1883          # 原生 MQTT 端口
    sslMqtt: 8883       # 原生 MQTT 的 SSL 端口
    websocket: 8083     # WebSocket 端口（路径 /mqtt）
    sslWebsocket: 8084  # WebSocket 的 SSL 端口

  # ===== 会话与重发 =====
  session:
    timeout: 60         # 会话超时时间（秒），超时未重连则清理会话
    delay: 10           # ACK 未确认后的重发延迟（秒）
    maxRetry: 3         # QoS 1/2 消息最大重试次数，超限后丢弃并释放资源

  # ===== SSL =====
  ssl:
    enabled: false      # 是否开启 SSL（开启后 mqtt/websocket 端口启用 TLS）
    certPath:           # 证书路径
    privatePath:        # 私钥路径

  # ===== 认证 =====
  auth:
    enabled: true       # 是否开启认证，false 表示关闭（关闭后任意客户端可连接）
    user:               # FIXED 本地用户列表（需引入 tlmqtt-authentication-fixed）
      - username: watson
        password: 12345
        id: 1
      - username: zhouhs
        password: 12345
        id: 2

  # ===== 通道（流量控制与整形） =====
  channel:
    writeLimit: 104857600   # 全局出站带宽限制（字节/秒），默认 100MB/s
    readLimit: 52428800     # 全局入站带宽限制（字节/秒），默认 50MB/s
    checkInterval: 1000     # 统计周期（毫秒）
    maxTime: 20971520       # 最大突发流量（字节），默认 20MB
    lowWaterMark: 32768     # 低水位线
    highWaterMark: 65536    # 高水位线

  # ===== 业务线程池 =====
  business:
    corePoolSize: 16     # 核心线程数（默认 CPU 核数）
    maxPoolSize: 32      # 最大线程数（默认 CPU 核数 * 2）
    queueCapacity: 1000  # 任务队列容量
    keepAliveSeconds: 60 # 非核心线程存活时间（秒）

  # ===== 线程模型 =====
  bossThreadSize: 1                          # Netty boss 线程数
  workThreadSize: 8                          # Netty work 线程数（默认 CPU 核数）
```

---

## 模块与依赖

| 功能 | 依赖 artifactId | 是否必须 |
| --- | --- | --- |
| 核心 Broker（自动配置、协议处理、转发） | `tlmqtt-spring-boot-starter` | 是 |
| 本地配置认证（FIXED） | `tlmqtt-authentication-fixed` | 按需 |
| HTTP 接口认证 | `tlmqtt-authentication-http` | 按需 |
| SQL（MySQL）认证 | `tlmqtt-authentication-sql` | 按需 |
| 文件 ACL 授权 | `tlmqtt-authorization-file` | 按需 |

所有模块 groupId 均为 `io.github.zhsqjm`，版本均为 `1.2.0.1`。

例如同时需要「本地配置认证 + 文件授权」：

```xml
<dependency>
    <groupId>io.github.zhsqjm</groupId>
    <artifactId>tlmqtt-authentication-fixed</artifactId>
    <version>1.2.0.1</version>
</dependency>
<dependency>
    <groupId>io.github.zhsqjm</groupId>
    <artifactId>tlmqtt-authorization-file</artifactId>
    <version>1.2.0.1</version>
</dependency>
```

---

## 认证 Authentication

认证通过 SPI 加载多个认证器（FIXED / HTTP / SQL），**采用责任链模式，只要任一认证器通过即可**。

### 开启 / 关闭认证

```yaml
tlmqtt:
  auth:
    enabled: true   # true=开启（默认），false=关闭
```

> **重要**：当 `auth.enabled=true`（默认）时，必须引入至少一个认证模块（fixed/http/sql）并配置认证源，否则所有客户端都会被拒绝。若不需要认证，请显式设置 `auth.enabled: false`。

### 3.1 本地配置认证（FIXED）

引入 `tlmqtt-authentication-fixed`，在配置中直接写用户名密码：

```yaml
tlmqtt:
  auth:
    enabled: true
    user:
      - username: watson
        password: 12345
        id: 1
      - username: zhouhs
        password: 12345
        id: 2
```

### 3.2 HTTP 接口认证

引入 `tlmqtt-authentication-http`。HTTP 认证通过实现 `AuthenticationService` 接口，在 `init()` 中返回 `HttpEntityInfo` 列表来配置认证源。**判断依据是接口返回的 HTTP 状态码为 200 即认证通过**。

```java
@Configuration
public class HttpAuthConfig {
    @Bean
    public AuthenticationService authenticationService() {
        return new AuthenticationService() {
            @Override
            public Map<AuthenticationType, List<TlAuthenticationSubject>> init() {
                HttpEntityInfo http = new HttpEntityInfo(1L);
                http.setUrl("http://your-auth-server.com/login");
                http.setMethod("POST");                     // GET 或 POST
                http.setParams(new HashMap<>() {{
                    put("username", "uname");               // 请求参数别名映射
                    put("password", "pwd");
                }});
                // 可选：自定义请求头
                // http.setHeaders(new HashMap<>() {{ put("Authorization", "Bearer xxx"); }});

                Map<AuthenticationType, List<TlAuthenticationSubject>> map = new HashMap<>();
                map.put(AuthenticationType.HTTP, Collections.singletonList(http));
                return map;
            }

            @Override public void save(TlAuthenticationSubject subject) { }
            @Override public void delete(TlAuthenticationSubject subject) { }
            @Override public List<TlAuthenticationSubject> getByType(AuthenticationType type) { return Collections.emptyList(); }
        };
    }
}
```

### 3.3 SQL（MySQL）认证

引入 `tlmqtt-authentication-sql`。同样通过实现 `AuthenticationService`，在 `init()` 中返回 `SqlEntityInfo`。默认执行 SQL 为：

```sql
SELECT 1 FROM {table} WHERE {usernameColumn} = ? AND {passwordColumn} = ? LIMIT 1
```

```java
SqlEntityInfo sql = new SqlEntityInfo(1L);
sql.setHost("127.0.0.1");
sql.setPort("3306");
sql.setUsername("root");
sql.setPassword("root");
sql.setDatabase("your_db");
sql.setTable("mqtt_user");
sql.setUsernameColumn("username");
sql.setPasswordColumn("password");
// 可选：自定义 SQL（占位符为 ?,?），不设置则使用上面的默认 SQL
// sql.setSql("SELECT 1 FROM mqtt_user WHERE username = ? AND password = ? LIMIT 1");

map.put(AuthenticationType.SQL, Collections.singletonList(sql));
```

---

## 授权 ACL

引入 `tlmqtt-authorization-file` 后，Broker 会在 classpath 根目录读取 `acl.conf` 文件进行发布 / 订阅权限校验。

### 规则格式

```plain
类型:值1,值2 | topic: 主题 | 操作 | 权限
```

- **类型**：`user`（用户名）、`client`（客户端 ID）、`ip`（客户端 IP）
- **操作**：`sub`（订阅）、`pub`（发布）
- **权限**：`allow`（允许）、`deny`（拒绝）
- 支持通配符 `*`，未匹配到任何规则时的兜底规则写在最后

### 示例

```plain
# 不允许用户名 admin 的客户端订阅 a/b 主题
user:admin | topic: a/b | sub | deny
# 允许用户名 admin 的客户端订阅 a/b 主题
user:admin | topic: a/b | sub | allow
# 不允许用户名 admin 的客户端向 topic/# 发布消息
user:admin | topic: topic/# | pub | deny
# 不允许客户端 ID 为 c1、c2 的客户端订阅 a/v 主题
client:c1,c2 | topic: a/v | sub | deny
# 不允许 IP 为 127.0.0.1 的客户端向 a/b 发布消息
ip: 127.0.0.1 | topic: a/b | pub | deny
# 允许任何用户发布/订阅任何主题（未匹配到规则时走这条兜底）
user: * | topic: * | * | allow
```

> 将 `acl.conf` 放到自己项目的 `src/main/resources/` 目录下即可覆盖默认规则。

---

## WebSocket 与 SSL

### WebSocket 连接

WebSocket 的 MQTT 端点为 **`/mqtt`**，默认端口 `8083`：

```
ws://localhost:8083/mqtt
```

支持的子协议：`mqtt`、`mqttv3.1`、`mqttv3.1.1`。

### SSL / TLS

设置 `tlmqtt.ssl.enabled=true` 并配置证书与私钥后：

- 原生 MQTT 的 TLS 端口为 `sslMqtt`（默认 8883）
- WebSocket 的 TLS 端口为 `sslWebsocket`（默认 8084）

```yaml
tlmqtt:
  ssl:
    enabled: true
    certPath: /path/to/cert.crt
    privatePath: /path/to/private.pem
```

---

## QoS 消息流程

| QoS | 发布方 → Broker | Broker → 订阅方 | 语义 |
| --- | --- | --- | --- |
| 0 | 直接转发，无确认 | 直接发送，无确认 | 至多一次 |
| 1 | PUBLISH → 回 PUBACK | 发送 PUBLISH，未收到 PUBACK 按 `session.delay` 重发，最多 `session.maxRetry` 次 | 至少一次 |
| 2 | PUBLISH → 回 PUBREC，收到 PUBREL → 回 PUBCOMP | 发送 PUBLISH，收到 PUBREC 后发送 PUBREL，未收到 PUBCOMP 重发 | 恰好一次 |

- QoS 2 的入站消息会先暂存（`inboundCache`），收到客户端 PUBREL 后再真正转发，防止重复投递。
- 转发时 Broker 会为每个订阅者独立分配 16 位 messageId，并与入站 messageId 隔离，避免自订阅场景下的 ID 冲突。
- 超出 `session.maxRetry` 次仍未确认的消息会被丢弃，同时释放 messageId 与 In-Flight 窗口，避免资源泄漏。

---

## 主题通配符

订阅主题时支持两种通配符：

- **单层通配符 `+`**：匹配主题的一个层级。例如 `home/+/temperature` 可匹配 `home/livingroom/temperature`、`home/kitchen/temperature`。
- **多层通配符 `#`**：匹配剩余的所有层级。例如 `home/#` 可匹配 `home/livingroom/temperature`、`home/kitchen/humidity`。

> 注意：通配符只能用于**订阅**，不能用于**发布**。

---

## 共享订阅

共享订阅将同一主题的消息在多个订阅者之间**负载均衡**分发（同一条消息只会投递给组内一个订阅者）。

```
$share/{group}/{topic}
```

此外还支持队列订阅：

```
$queue/{topic}
```

默认使用随机策略选择订阅者，可通过实现 `IShareSubscribeClientChoose` 自定义选择策略（见 [扩展点](#扩展点)）。

---

## 扩展点

### 1. 自定义持久化（实现接口并注册为 Bean）

tlmqtt 默认使用内存（Caffeine）进行数据持久化。所有存储接口都通过 `@ConditionalOnMissingBean` 提供默认实现，**只需实现对应接口并注册为 Spring Bean 即可替换**：

| 接口 | 职责 |
| --- | --- |
| `SessionService` | 会话持久化 |
| `PublishService` | QoS 1/2 消息与遗嘱消息持久化 |
| `RetainService` | 保留消息持久化 |
| `SubscriptionService` | 订阅关系持久化 |
| `ShareSubscribeService` | 共享订阅持久化 |
| `AuthenticationService` | 认证数据源（HTTP/SQL 认证源加载） |

```java
@Configuration
public class MyStoreConfig {
    @Bean
    public SessionService sessionService() {
        return new MyRedisSessionService(); // 换成你自己的实现
    }
}
```

### 2. 消息拦截器 `PublishInterceptor`

实现 `PublishInterceptor` 并注册为 Bean，即可在消息发布前进行拦截、修改或终止处理。

```java
@Component
public class MyInterceptor implements PublishInterceptor {
    @Override
    public int order() { return 0; } // 越小越先执行

    @Override
    public TlMqttPublishReq intercept(ChannelHandlerContext ctx, TlMqttPublishReq req, TlMqttSession session) {
        // 返回 null 表示终止处理（不再转发）
        // 返回修改后的 req 表示继续处理
        return req;
    }
}
```

### 3. 共享订阅选择策略 `IShareSubscribeClientChoose`

实现该接口并注册为 Bean，可自定义共享订阅时选择哪个订阅者接收消息。

```java
@Component
public class MyShareChoose implements IShareSubscribeClientChoose {
    @Override
    public TlSubClient choose(List<TlSubClient> subClients, String content) {
        // 自定义选择逻辑
        return subClients.get(0);
    }
}
```

### 4. 自定义认证 / 授权提供者（SPI）

- **认证**：实现 `TlAuthenticationProvider`，通过 SPI（`META-INF/services`）注册，返回自定义的 `AbstractTlAuthentication`。
- **授权**：实现 `TlAuthorizationProvider`，通过 SPI 注册，自定义发布 / 订阅权限校验逻辑。

---

## 感谢

+ [https://github.com/Wizzercn/MqttWk](https://github.com/Wizzercn/MqttWk)
+ [https://github.com/quickmsg/smqttx](https://github.com/quickmsg/smqttx)

`tlmqtt` 致力于为物联网开发者提供轻量、高效的 MQTT 消息服务，如果您觉得还不错，请在右上角点一下 star，大家的支持是开源最大的动力。
