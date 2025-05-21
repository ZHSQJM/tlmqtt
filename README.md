# tlmqtt

#### 模块说明
- tlmqtt-auth 认证模块
- tlmqtt-boot springboot项目
- tlmqtt-bridge 数据桥接
- tlmqtt-common 公共模板
- tlmqtt-core 核心方法
- tlmqtt-store 数据存储
#### 功能
- 完全独立的mqtt3.1.1 协议解析
- qos 0,1,2的消息支持
- session的持久化
- topic的过滤消息转发
- 基于本地文件，数据库和http接口认证
- 数据桥接功能，支持kafka，mysql等
#### 后续功能
- acl 权限控制
- 页面展示
- 数据桥接
- 共享订阅
- 系统订阅
- mqtt5.0协议支持
- 集群

基于java的一款轻量级的mqtt broker，底层基于netty与project reactor
可扩展，可定制化

#### 配置文件说明(common的resources目录下)
```yaml
session:
  timeout: 5 #session会话超时时间 如果过了这个时间还没连接 那么就不保持会话
  delay: 5 #ack消息确定 30s后没有收到确定就重发
port:
  mqtt: 18883 # mqtt的默认端口
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
#### 启动示例
##### 启动mqtt服务 
```java

  TlBootstrap bootstrap = new TlBootstrap();
  bootstrap.setServer(TlMqttServer.class).start();

```
##### 启动websocket服务
```java

  TlBootstrap bootstrap = new TlBootstrap();
  bootstrap.setServer(TlWebSocketServer.class).start();

```
上述启动方式使用的就是配置文件中默认的配置

```java
  TlBootstrap bootstrap = new TlBootstrap();
  bootstrap.setServer(TlWebSocketServer.class)
            .setDelay(1) //重发消息延迟时间
           .setPort(18883) //启动的端口
            .setAuth(true) //是否开启认证
            .setHttpEntity(httpEntity) //http认证集合
            .setSqlEntity(sqlEntity) //sql认证集合
            .setFixUser(fixUsers) //固定认证集合
         //   存储配置
            .setPublishService(new MemoryPublishServiceImpl()) //publish消息的存储
            .setRetainService(new MemoryRetainServiceImpl()) //retain消息的存储
            .setPubrelService(new MemoryPubrelServiceImpl()) // pubrel消息的存储
            .setSessionService(new MemorySessionServiceImpl()) // session消息的存储
           .setSsl(true) //是否开启ssl
            .setCertPath("") //ssl证书
            .setPrivatePath("") //ssl私钥
           .addAuthEntity(sqlEntityInfo) //添加认证实体
            .addAuthEntity(httpInfo) //添加认证实体

            .addKafkaBridge(new TlKafkaInfo()) // 添加kafka配置
             .addMysqlBridge(new MySqlInfo()) //添加mysql配置
            .addBridgeEntity(new KafkaBridgeObserver()) //添加桥接器
           .addAuthentication(new NoneA())//添加认证器
```
