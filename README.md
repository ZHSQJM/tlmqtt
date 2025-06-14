# tlmqtt
`tlmqtt是一款基于Java开发的轻量级高并发MQTT Broker，采用Netty和Project Reactor实现异步通信，完整支持MQTT 3.1.1协议，包括QoS消息分级、主题通配符、消息持久化等核心功能。项目提供认证（文件/数据库/HTTP）、数据桥接（Kafka/MySQL）和存储（内存/Redis）等可扩展组件，支持MQTT和WebSocket双协议接入。具备生产级特性如SSL加密、会话恢复及高并发处理能力，适用于物联网和实时通信场景。开发者可自定义认证逻辑和存储方案`
## 功能
- MQTT3.1.1协议自主解析
- 完整的qos 0,1,2的消息支持
- 遗嘱消息, 保留消息及消息分发重试
- SSL方式连接(可选择是否开启)
- websocket双协议支持
- 主题过滤
- 消息的持久化
- 基于文件，数据库，http接口认证
- 基于文件的acl订阅/发布权限控制
- 数据转发功能，目前支持kafka，mysql
## 快速开始
```
TlBootstrap bootstrap = new TlBootstrap();
bootstrap
.socket();//开启mqtt协议 默认端口1883
.websocket()//开启websocket协议 默认端口8083
.start();
````
## 后续功能
- 系统订阅
- mqtt5.0协议支持
- 页面展示
- 集群
### 配置文件说明(common的resources目录下)
```yaml
session:
  timeout: 5 #session会话超时时间 如果过了这个时间还没连接 那么就不保持会话
  delay: 5 #ack消息确定 5s后没有收到确定就重发
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
### 基础功能
#### 1. 会话的持久化
对于cleansession为0的会话，在CONNECT时，会查询上次是否已经存在了该会话，如果存在，那么就会恢复上次会话的状态,如果不存在，那么就创建一个新的会话
```java
   .then(storeManager.getSessionService().find(clientId))
   .switchIfEmpty(Mono.defer(() -> createNewSession(clientId, cleanSession, username, ctx)))
   .flatMap(session -> completeSessionHandling(session, req, ctx, cleanSession))
```
#### 2. 通配符匹配
在订阅主题时，可以使用通配符来订阅多个主题。有两种通配符：
- 单层通配符（+）：匹配主题层级中的任意值。例如，home/+/temperature可以匹配home/livingroom/temperature和home/kitchen/temperature。
- 多层通配符（#）：匹配多个层级。例如，home/#可以匹配home/livingroom/temperature和home/kitchen/humidity。
  注意：通配符只能用于订阅操作，不能用于发布消息。
####  3 认证
目前支持文件,http接口以及mysql数据库认证,可同时启用,只要任何一种认证通过即可
##### 3.1 开启或关闭认证。默认开启认证
```java
bootstrap.setAuth(false);
```
##### 3.2 基于文件的认证
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
##### 3.3 基于http接口
```java
ArrayList<HttpEntityInfo> httpEntityInfos = new ArrayList<>();
bootstrap.setHttpEntity(httpEntityInfos);
```
##### 3.4 基于mysql数据库认证
```yaml
bootstrap.setSqlEntity(new ArrayList<SqlEntityInfo>())
```
##### 3.5 自定义认证
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
#### 4 ACL权限控制(tl-auth的resource目录下)
tlmqtt自定义了一套专属的acl文件格式 并通过初始化时进行解析,具体格式如下
```
# 格式说明：类型:值1，值2 | 资源类型:资源 | 操作 | 权限
# 不允许用户名admin客户端订阅a/b主题
user:admin | topic: a/b | sub | deny
# 允许用户名admin客户端订阅a/b主题
user:admin | topic: a/b | sub | allow
# 不允许用户名admin客户端向topic/#主题发布消息
user:admin | topic: topic/# | pub | deny
# 不允许客户端ID为c1和c2客户端订阅a/v主题
client:c1,c2 | topic: a/v | sub | deny
# 不允许ip为127.0.0.1客户端向a/b主题发布消息
ip: 127.0.0.1 | topic: a/b | pub | deny
# 允许任何用户发布订阅任何主题 如果没有匹配到 折都是这条消息
user: * | topic: * | * | allow
```
#### 5. 数据桥接
tlmqtt目前支持将消息转发到mysql和kafka中，并提供接口用于用户自定义数据桥接
##### 5.1 数据库
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
##### 5.2 kafka
```java

TlKafkaInfo kafkaInfo = new TlKafkaInfo("ws", "172.28.33.102:9092",
"org.apache.kafka.common.serialization.StringSerializer",
"org.apache.kafka.common.serialization.StringSerializer");
bootstrap.addBridgeKafka(kafkaInfo);
```
#### 6. 存储
``tlmqtt``默认会话与消息都存储在内存中,当然用户也可以实现接口自定义
- SessionService 会话存储接口
- PublishService publish消息与遗嘱消息接口
- PubRelService pubrel消息接口
- RetainService 保留消息接口
```java
// 自定义存储为redis
bootstrap.setSessionService(redisSessionService).setPublishService(redisPublishService)
```
#### 7. 保留消息
设置发布的消息为保留消息后，当有客户端订阅这个主题时，就会收到保留消息
#### 8. 遗嘱消息
在客户端非正常断开后 发送遗嘱消息
# 感谢项目
	- <https://github.com/Wizzercn/MqttWk>
	- <https://github.com/quickmsg/smqttx>
`tlmqtt致力于为物联网开发者提供轻量、高效的 MQTT 消息服务，如果您觉得还不错请在右上角点一下 star大家的支持是开源最大动力`
