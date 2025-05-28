# tlmqtt

## 模块说明
- tlmqtt-auth 认证模块
- tlmqtt-bridge 数据桥接
- tlmqtt-common 公共模板
- tlmqtt-core 核心方法
- tlmqtt-store 数据存储
## 功能
- 完整的mqtt3.1.1 协议解析
- qos 0,1,2的消息支持
- topic的过滤消息转发
- 消息的持久化
- 基于本地文件，数据库和http接口认证
- 数据桥接功能，支持kafka，mysql等
## 后续功能
- acl 权限控制
- 页面展示
- 共享订阅
- 系统订阅
- mqtt5.0协议支持
- 集群

基于java的一款轻量级的mqtt broker，底层基于netty与project reactor 可扩展，可定制化

### 配置文件说明(common的resources目录下)
```yaml
session:
  timeout: 5 #session会话超时时间 如果过了这个时间还没连接 那么就不保持会话
  delay: 5 #ack消息确定 30s后没有收到确定就重发
port:
  mqtt: 1883 # mqtt的默认端口
  sslMqtt: 8883 # mqtt的ssl断开
  websocket: 8083 #websocket的端口
  sslWebsocket: 8084 #websocket的ssl断开
ssl:
  enabled: false #是否开启ssl
  certPath: C:\Users\knn\Desktop\fsdownload\cret.crt #ssl证书
  privatePath: C:\Users\knn\Desktop\fsdownload\private.pem #`
auth:
  enabled: true #是否开启认证 false就是关闭认证
  user: #开启认证后fix的认证信息
    - username: watson
      password: 12345
    - username: zhouhs
      password: 12345
```
### 启动示例
#### mqtt服务 
```java
TlBootstrap bootstrap = new TlBootstrap();
bootstrap.setServer(TlMqttServer.class).start();

```
#### websocket服务
```java
TlBootstrap bootstrap = new TlBootstrap();
bootstrap.setServer(TlWebSocketServer.class).start();

```
上述启动方式使用的就是配置文件中默认的配置

### 基础功能
#### 1. 会话的持久化
对应cleansession为0的会话，在下次再次连接后会重新发送qos1和qos2的消息
#### 2. 通配符匹配
支持mqtt的主题通配符的匹配
#### 3. qos1与qos2的消息
对于qos1与qos2的消息完全支持消息重发，默认5秒钟没有收到回复后就会重发
#### 4. 保留消息
对于保留消息，支持新的主题订阅后发送保留消息
#### 5. 遗嘱消息
在客户端非正常断开后 发送遗嘱消息


### 功能描述
#### 1. 认证
目前支持文件,http接口以及mysql数据库认证,可同时启用,只要任何一种认证通过即可
##### 1.1 开启或关闭认证。默认开启认证
```java
bootstrap.setAuth(false);
```
##### 1.2 基于文件的认证
- 声明式
```yaml
auth:
  user: 
    - username: watson
      password: 12345
    - username: zhouhs
      password: 12345
```
- 编程式
```java
bootstrap.setFixUser(Collections.singletonList(new TlUser("mqtt","mqtt")))
```
##### 1.3 基于http接口
```java
ArrayList<HttpEntityInfo> httpEntityInfos = new ArrayList<>();
bootstrap.setHttpEntity(httpEntityInfos);
```
##### 1.4 基于mysql数据库认证
```yaml
bootstrap.setSqlEntity(new ArrayList<SqlEntityInfo>())
```
##### 1.5 自定义认证
继承AbstractTlAuthentication方法
```yaml
public class NoneA extends AbstractTlAuthentication {
    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void add(Object object) {

    }
}
bootstrap.addAuthentication(new NoneA());
```

#### 2. 数据桥接
tlmqtt目前支持将消息转发到mysql和kafka中，并提供接口用于用户自定义数据桥接
##### 2.1 数据库
```java
TlMySqlInfo mySqlInfo = new TlMySqlInfo();
mySqlInfo.setHost("127.0.0.1");
mySqlInfo.setPort(3306);
mySqlInfo.setUsername("root");
mySqlInfo.setPassword("kangni");
mySqlInfo.setDatabase("watson");
mySqlInfo.setTable("mqtt_msg");
mySqlInfo.setDriverClassName("com.mysql.cj.jdbc.Driver");
bootstrap.addBridgeMysql(mySqlInfo);
```
##### 2.2 kafka
```java

TlKafkaInfo kafkaInfo = new TlKafkaInfo("ws", "172.28.33.102:9092",
"org.apache.kafka.common.serialization.StringSerializer",
"org.apache.kafka.common.serialization.StringSerializer");
bootstrap.addBridgeKafka(kafkaInfo);
```

##### 2.3 自定义
```java
public class HttpBridge implements EventHandler<PublishMessage> {
    @Override
    public void onEvent(PublishMessage event, long sequence, boolean endOfBatch) throws Exception {
        
    }
}
bootstrap.addHandler(new HttpBridge());
```

#### 3. 存储
tlmqtt默认会话与消息都存储在内存中,当然用户也可以实现接口自定义
- SessionService
- PublishService
- PubRelService
- RetainService
```java
bootstrap.setSessionService(redisSessionService).setPublishService(redisPublishService)
```
