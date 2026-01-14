
<h1 align="center">TLMQTT</h1>
<p align="center">
  <a href="https://github.com/quickmsg/smqttx/blob/release/ignite/README.md">
    <img alt="apache" src="https://img.shields.io/badge/license-Apache%202-blue"/>
  </a>
  <a href="https://projectreactor.io/docs/netty/release/reference/index.html">
    <img alt="netty" src="https://img.shields.io/badge/netty-4.2.8.Final-green"/>
  </a>
  <a href="https://projectreactor.io/docs/core/release/reference/">
    <img alt="project-reactor" src="https://img.shields.io/badge/projectreactor-3.4.34-yellow"/>
  </a>
  <a href="https://projectreactor.io/docs/core/release/reference/">
    <img alt="caffeine" src="https://img.shields.io/badge/caffeine-3.2.0-yellow"/>
  </a>
  <a href="https://projectreactor.io/docs/netty/release/reference/index.html">
    <img alt="ignite" src="https://img.shields.io/badge/mqtt-3.1.1-green"/>
    <img alt="ignite" src="https://img.shields.io/badge/mqtt-5.0-blue"/>  
</a>
</p>

`tlmqtt是一款基于Java开发的轻量级高性能的嵌入式MQTT Broker,用于快速将任何SpringBoot应用搭建成MQTT服务`.
## 核心功能
### 协议支持
- **标准MQTT协议** -完整支持MQTT3.1.1 & 5.0协议
- **WebSocket协议** - 支持基于WebSocket的MQTT通信
- **SSL/TLS** - 支持TLS单向与双向加密
### 服务质量
- **QOS0** - 无需确认消息
- **QOS1** - 至少一次消息确认
- **QOS2** - 至少一次消息确认
###  功能
- **会话持久化** - 会话持久化，断开连接后，会话仍然存在，下次连接时，会恢复会话状态
- **Topic过滤** - 支持通配符匹配
- **保留消息，遗嘱消息**
- **自定义拦截器**
- **共享订阅**
- **认证** - 基于本地配置/HTTP/SQL的认证
- **授权** - 基于文件的权限控制

## 快速开始
### **开发环境** SpringBoot >=2.7.4 Java >=1.8
```xml 
  <dependency>
        <groupId>io.github.zhsqjm</groupId>
        <artifactId>tlmqtt-core</artifactId>
        <version>1.1.0</version>
  </dependency>
```
### 配置文件
在`application.yml`中可选择添加配置
```yaml
session:
  timeout: 5 #session会话超时时间 如果过了这个时间还没连接 那么就不保持会话
  delay: 5 #ack消息确定 5s后没有收到确定就重发
  maxRetry: 3 #qos1和qos2的消息重试次数
port:
  mqtt: 1883 # mqtt的默认端口
  sslMqtt: 8883 # mqtt的ssl端口
  websocket: 8083 #websocket的端口
  sslWebsocket: 8084 # websocket的ssl端口
ssl:
  enabled: true # 是否开确认中，默认开启
  certPath: C:\Users\knn\Desktop\fsdownload\cret.crt #证书地址
  privatePath: C:\Users\knn\Desktop\fsdownload\private.pem #私钥
auth:
  enabled: true #是否开启认证 false就是关闭认证
  user: #开启认证后fix的认证信息
    - username: watson
      password: 12345
      id: 1
    - username: zhouhs
      password: 12345
      id: 2
# 通道设置
channel:
  writeLimit: 104857600 # 全局出站带宽限制：100MB/s
  readLimit: 52428800 #  全局入站带宽限制：50MB/s
  checkInterval: 1000  #统计周期：1秒
  maxTime: 20971520 # `最大突发流量：20MB
  lowWaterMark: 65536 # 默认 32768
  highWaterMark: 131072 #  默认 65536
#业务线程池队列配置
business:
  core: 16 # 核心线程数
  max: 32 # 业务线程数
  queue: 10000 #任务队列
  keepAlive: 60 # 非核心线程数的存活时间
```
### 应用启动
在springBoot启动类上添加`@EnableTlMqtt`
```java
@SpringBootApplication
@EnableTlMqtt
public class MqttApplication {
    
}
```
启动成功后，即可通过mqtt协议进行通信。

## 后续功能
+ 系统订阅
+ 页面展示
+ 集群


### 基础功能
#### 1. 数据持久化
对于连接的会话以及保留消息的数据持久化，tlmqtt默认使用内存进行数据持久化。如果需要自定义数据的持久化方式，tlmqtt提供了不同数据的接口，用户只需要
实现该接口就可以实现自己的数据持久化方式。
SessionService -会话的持久化方式
PublishService - qos1与qos2的消息持久化方式
RetainService - 保留消息的持久化方式
ShareSubscribeService - 共享订阅的持久化方式
SubscriptionService - 订阅的持久化方式
#### 2. 通配符匹配
在订阅主题时，可以使用通配符来订阅多个主题。有两种通配符：

+ 单层通配符（+）：匹配主题层级中的任意值。例如，home/+/temperature可以匹配home/livingroom/temperature和home/kitchen/temperature。
+ 多层通配符（#）：匹配多个层级。例如，home/#可以匹配home/livingroom/temperature和home/kitchen/humidity。  
注意：通配符只能用于订阅操作，不能用于发布消息。

#### 3 认证
目前支持文件,http接口以及mysql数据库认证,可同时启用,只要任何一种认证通过即可
##### 3.1 开启或关闭认证。默认开启认证


##### 3.2 基于文件的认证
```yaml
auth:
  user: 
    - username: watson
      password: 12345
      id: 1
    - username: zhouhs
      password: 12345
      id: 2
```
+ 编程式

#### 4 ACL权限控制(tl-auth的resource目录下)
tlmqtt自定义了一套专属的acl文件格式 并通过初始化时进行解析,具体格式如下

```plain
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


# 感谢项目
+ [https://github.com/Wizzercn/MqttWk](https://github.com/Wizzercn/MqttWk)
+ [https://github.com/quickmsg/smqttx](https://github.com/quickmsg/smqttx)

`tlmqtt致力于为物联网开发者提供轻量、高效的 MQTT 消息服务，如果您觉得还不错请在右上角点一下 star大家的支持是开源最大动力`

