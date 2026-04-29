# TL-MQTT 项目整改清单

> 生成时间: 2026-04-27
> 项目路径: E:\project\origin\tl-mqtt

---

## P0 - 必须修复

### 1. 规则引擎核心逻辑被注释（功能缺失）
- **文件**: `tlmqtt-rule/src/main/java/com/tlmqtt/rule/RuleEngineProcessor.java`
- **位置**: 第24-37行
- **问题**: Topic匹配和条件表达式判断代码被注释，导致规则引擎形同虚设
- **当前代码**:
  ```java
  public void processMessage(String topic, TlMqttPublishReq req) {
      for (RuleDefinition rule : rules) {
          // if (!topicMatches(rule.getTopicFilter(), topic)) continue;
          // Boolean isMatch = (Boolean) AviatorEvaluator.execute(rule.getCondition(), payload);
          // if (isMatch) {
              List<ActionSink> sinks = rule.getSinks();
              handler(sinks, req);
          // }
      }
  }
  ```
- **整改**:
  1. 取消注释，恢复完整逻辑
  2. 实现 `topicMatches()` 方法支持 MQTT 主题通配符（+和#）
  3. 使用 AviatorEvaluator 执行条件表达式
  4. 添加 payload 变量映射
- **优先级**: P0

### 2. MySqlSink 语法错误和空实现
- **文件**: `tlmqtt-rule/src/main/java/com/tlmqtt/rule/sink/MySqlSink.java`
- **位置**: 第24行
- **问题**: 构造函数语法错误，process方法是空实现
- **当前代码**:
  ```java
  public MySqlSink(String host,)  // 语法错误
  @Override
  public void process(TlMqttPublishReq req) {
      // 空实现
  }
  ```
- **整改**:
  1. 修复构造函数签名
  2. 实现完整的 MySQL 写入逻辑
  3. 使用连接池管理数据库连接
  4. 处理 SQL 参数化和异常
- **优先级**: P0

---

## P1 - 重要修复

### 3. Session状态初始化不安全
- **文件**: `tlmqtt-common/src/main/java/com/tlmqtt/common/model/TlMqttSession.java`
- **位置**: 第110-122行
- **问题**: `@Builder` 模式下，final 字段的类内初始化会被忽略，导致 messageIdManager、inFlightCount、messageQueue 为 null
- **当前代码**:
  ```java
  private final MessageIdManager messageIdManager = new MessageIdManager();
  private final AtomicInteger inFlightCount = new AtomicInteger(0);
  private final Queue<TlMqttPublishReq> messageQueue = new ConcurrentLinkedQueue<>();
  ```
- **整改方案**:
  ```java
  // 方案1: 使用自定义Builder
  public static class TlMqttSessionBuilder {
      private final MessageIdManager messageIdManager = new MessageIdManager();
      private final AtomicInteger inFlightCount = new AtomicInteger(0);
      private final Queue<TlMqttPublishReq> messageQueue = new ConcurrentLinkedQueue<>();
  }

  // 方案2: 在getter中延迟初始化
  private MessageIdManager messageIdManager;
  private AtomicInteger inFlightCount;
  private Queue<TlMqttPublishReq> messageQueue;

  private MessageIdManager getMessageIdManager() {
      if (messageIdManager == null) this.messageIdManager = new MessageIdManager();
      return messageIdManager;
  }
  ```
- **优先级**: P1

### 4. KeepAlive 1.5倍超时未实现
- **文件**: `tlmqtt-core/src/main/java/com/tlmqtt/core/handler/TlConnectHandler.java`
- **位置**: 第352行
- **问题**: TODO说明 MQTT 规范要求1.5倍 keepAlive 时间内没收到报文则断开连接，但当前只设置了 keepAlive 秒超时
- **当前代码**:
  ```java
  //todo 如果保持连接的值非零，并且服务端在1.5倍的保持连接时间内没有收到客户端的控制报文，
  //它必须断开客户端的网络连接，并判定网络连接已断开 [MQTT-3.1.2-22]
  ctx.pipeline().addLast(new IdleStateHandler(0, 0, keepAlive, TimeUnit.SECONDS));
  ```
- **整改**:
  1. 实现自定义 IdleStateHandler 或使用 HashedWheelTimer
  2. 检测1.5倍 keepAlive 时间内是否有收到任何控制报文
  3. 区分「真正断开」和「仅读空闲」
- **优先级**: P1

### 5. ByteBuf引用计数管理混乱
- **文件**: 多处
- **位置**:
  - `ForwardMessageService.java:85, 109-110, 275`
  - `TlPublishHandler.java:111, 116`
- **问题**: `ReferenceCountUtil.retain()` 和 `release()` 散落在多处，难以追踪，容易泄漏或过早释放
- **整改**:
  1. 创建 `ByteBufManager` 工具类统一管理引用计数
  2. 使用 try-with-resources 或 finally 块确保释放
  3. 添加日志追踪引用计数变化
  ```java
  public class ByteBufManager {
      public static ByteBuf retainAndGet(ByteBuf buf) {
          return buf.retain();
      }
      public static void safeRelease(ByteBuf buf) {
          if (buf != null && buf.refCnt() > 0) {
              ReferenceCountUtil.safeRelease(buf);
          }
      }
  }
  ```
- **优先级**: P1

---

## P2 - 优化改进

### 6. ForwardMessageService 耦合过重
- **文件**: `tlmqtt-core/src/main/java/com/tlmqtt/core/service/ForwardMessageService.java`
- **位置**: 第40-69行
- **问题**: 一个Service依赖8个其他服务，违背单一职责原则
- **当前依赖**:
  ```java
  private final AliasService aliasService;
  private final ShareSubscribeService shareSubscribeService;
  private final IShareSubscribeClientChoose shareSubscribeClientChoose;
  private final SubscriptionService subscriptionService;
  private final SessionService sessionService;
  private final PublishService publishService;
  private final TlChannelService channelService;
  private final TlSchedulerTaskService schedulerTaskService;
  ```
- **整改方案** - 拆分为多个职责单一的服务:
  ```
  ForwardMessageService (门面类，协调各服务)
      │
      ├── SubscriptionLookupService    # 只负责查询订阅者
      │       - findSubscribers(topic)
      │       - findShareSubscribers(topic)
      │
      ├── QoSFlowControlService        # 只负责流量控制
      │       - checkInFlight(session)
      │       - enqueue(session, message)
      │       - dequeue(session)
      │
      ├── MessageRetryService          # 只负责重试逻辑
      │       - scheduleWithRetry(type, clientId, message, count)
  ```
- **优先级**: P2

### 7. 认证链双重责任链设计
- **文件**:
  - `tlmqtt-authentication/base/src/main/java/com/tlmqtt/authentication/base/AuthenticationManager.java`
  - `tlmqtt-common/src/main/java/com/tlmqtt/common/authentication/AbstractTlAuthentication.java`
- **位置**: AuthenticationManager.java:26-39, AbstractTlAuthentication.java:22-28
- **问题**:
  1. AbstractTlAuthentication 已有责任链逻辑
  2. AuthenticationManager 又包装了 NoneAuthenticationService 作为头节点
  3. `next` 字段是 `protected`，违背封装
- **整改方案**:
  ```java
  // 方案1: 移除 AuthenticationManager 的包装，直接使用链
  // 方案2: 移除 AbstractTlAuthentication 的 execute 责任链，使用 Composite 模式

  // 推荐：使用 @Chain 注解 + BeanPostProcessor 自动构建链
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Chain {
      int order() default 100;
  }

  @Chain(order = 1)
  public class FixedAuthentication extends AbstractTlAuthentication { }

  @Chain(order = 2)
  public class HttpAuthentication extends AbstractTlAuthentication { }
  ```
- **优先级**: P2

### 8. 订阅处理线程安全问题
- **文件**: `tlmqtt-core/src/main/java/com/tlmqtt/core/handler/TlSubscribeHandler.java`
- **位置**: 第92行
- **问题**: 直接修改 `session.getTopics().addAll()` 没有同步
- **当前代码**:
  ```java
  Set<String> newTopicNames = topics.stream().map(TlTopic::getName).collect(Collectors.toSet());
  session.getTopics().addAll(newTopicNames);  // 非线程安全
  ```
- **整改方案**:
  ```java
  // 方案1: 使用 ConcurrentHashMap.newKeySet()
  private final Set<String> topics = ConcurrentHashMap.newKeySet();

  // 方案2: 添加同步锁
  synchronized (session.getTopics()) {
      session.getTopics().addAll(newTopicNames);
  }
  ```
- **优先级**: P2

---

## P3 - 改进建议

### 9. Store层样板代码重复
- **文件**: `tlmqtt-store/src/main/java/com/tlmqtt/store/service/impl/` 目录下多个实现类
- **问题**: 每个 Service 实现都是类似的 `Mono.fromSupplier(() -> { cache.put(); return value; })` 模式
- **整改**: 创建抽象基类
  ```java
  public abstract class AbstractCacheService<K, V> {
      protected abstract Cache<K, V> getCache();
      protected abstract K extractKey(V value);

      public Mono<V> save(V value) {
          return Mono.fromSupplier(() -> {
              getCache().put(extractKey(value), value);
              return value;
          });
      }

      public Mono<V> find(K key) {
          return Mono.justOrEmpty(getCache().getIfPresent(key));
      }
  }
  ```
- **优先级**: P3

### 10. 硬编码配置值
- **文件**: 多处
- **位置**:
  - `DefaultSessionServiceImpl.java:27` - `maximumSize(10000000)`
  - `DefaultPublishServiceImpl.java:32` - `maximumSize(100000000)`
  - `DefaultSubscriptionServiceImpl.java:28` - `expireAfterAccess(24, TimeUnit.HOURS)`
- **问题**: 缓存大小、过期时间等硬编码，难以调优
- **整改**:
  ```java
  // 在 application.yml 中配置
  tlmqtt:
    cache:
      session:
        max-size: 10000000
      publish:
        max-size: 100000000
        expire-hours: 24
  ```
- **优先级**: P3

### 11. SPI order默认值风险
- **文件**: `tlmqtt-common/src/main/java/com/tlmqtt/common/authentication/TlAuthenticationProvider.java`
- **位置**: 第25行
- **问题**: `default int order() { return 100; }` 如果 Provider 没重写则顺序不确定
- **整改**:
  ```java
  // 使用 @Order 注解强制要求
  default int order() { return Integer.MAX_VALUE; }  // 未定义放最后

  // 或使用必填注解
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Order {
      int value();
  }
  ```
- **优先级**: P3

### 12. 异常处理风格不统一
- **文件**: 多个 Handler
- **问题**: 有的用 `ctx.fireExceptionCaught()`，有的直接处理
- **整改**: 统一异常处理流程，所有业务异常都通过 `fireExceptionCaught` 触发 `TlExceptionHandler`
- **优先级**: P3

### 13. 未使用的导入
- **文件**: `tlmqtt-rule/src/main/java/com/tlmqtt/rule/rule/RuleDefinition.java`
- **位置**: 第7行
- **问题**: `import javax.xml.transform.Source;` 未使用
- **整改**: 删除未使用的导入
- **优先级**: P3

---

## 已删除文件清理

> 以下文件在 git status 中显示为已删除(D)，如确认无用应提交删除

```
D  tlmqtt-rule/src/main/java/com/tlmqtt/rule/ActionConfig.java
D  tlmqtt-rule/src/main/java/com/tlmqtt/rule/ActionExecutor.java
```

**建议**: 如果这些文件确实已废弃，执行 `git rm` 彻底删除

---

## 优先级汇总

| 优先级 | 问题 | 预计工时 |
|--------|------|----------|
| P0 | 规则引擎核心逻辑被注释 | 2h |
| P0 | MySqlSink语法错误 | 1h |
| P1 | Session状态初始化不安全 | 1h |
| P1 | KeepAlive 1.5倍超时未实现 | 3h |
| P1 | ByteBuf引用计数管理混乱 | 2h |
| P2 | ForwardMessageService耦合过重 | 4h |
| P2 | 认证链双重责任链 | 2h |
| P2 | 订阅处理线程安全 | 1h |
| P3 | Store层样板代码 | 2h |
| P3 | 硬编码配置值 | 1h |
| P3 | SPI order默认值风险 | 0.5h |
| P3 | 异常处理风格不统一 | 1h |
| P3 | 未使用的导入 | 0.1h |

---

## 附录：设计优点（保留）

1. **模块化架构清晰** - 各模块职责明确，依赖关系清晰
2. **SPI扩展机制完善** - 认证、授权、拦截器都支持插件化
3. **MQTT 5.0 支持完整** - 主题别名、共享订阅、用户属性等
4. **响应式编程** - 全面使用 Reactor，异步非阻塞
5. **高性能缓存策略** - Caffeine + Trie 树
6. **流量控制** - In-Flight窗口 + 背压队列
