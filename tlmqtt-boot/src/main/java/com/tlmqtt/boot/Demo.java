//package com.tlmqtt.boot;
//
//import com.tlmqtt.auth.http.HttpEntityInfo;
//import com.tlmqtt.auth.sql.SqlEntityInfo;
//import com.tlmqtt.bridge.db.TlMySqlInfo;
//import com.tlmqtt.common.model.entity.TlUser;
//import com.tlmqtt.core.TlBootstrap;
//import com.tlmqtt.core.TlWebSocketServer;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import static com.tlmqtt.auth.http.HttpMethod.GET;
//import static com.tlmqtt.auth.http.HttpMethod.POST;
//
///**
// * @Author: hszhou
// * @Date: 2025/5/16 14:55
// * @Description: 必须描述类做什么事情, 实现什么功能
// */
//public class Demo {
//
//    public static void main(String[] args) {
//
//
//        SqlEntityInfo sqlEntityInfo = new SqlEntityInfo();
//        sqlEntityInfo.setHost("127.0.0.1");
//        sqlEntityInfo.setPort("3306");
//        sqlEntityInfo.setUsername("root");
//        sqlEntityInfo.setPassword("kangni");
//        sqlEntityInfo.setDatabase("watson");
//        sqlEntityInfo.setTable("sys_user");
//        sqlEntityInfo.setUsernameColumn("username");
//        sqlEntityInfo.setPasswordColumn("password");
//        sqlEntityInfo.setDriverClassName("com.mysql.cj.jdbc.Driver");
//
//
//
//        TlMySqlInfo mySqlInfo = new TlMySqlInfo();
//        mySqlInfo.setHost("127.0.0.1");
//        mySqlInfo.setPort(3306);
//        mySqlInfo.setUsername("root");
//        mySqlInfo.setPassword("kangni");
//        mySqlInfo.setDatabase("watson");
//        mySqlInfo.setTable("mqtt_msg");
//
//        mySqlInfo.setDriverClassName("com.mysql.cj.jdbc.Driver");
//
//
//        HashMap<String, String> headers = new HashMap<>(16);
//        headers.put("Content-Type", "application/json");
//        HttpEntityInfo httpInfo = new HttpEntityInfo();
//        httpInfo.setUrl("http://127.0.0.1:8097/mqtt/login");
//        httpInfo.setMethod(POST);
//        httpInfo.setHeaders(headers);
//
//
//        List<HttpEntityInfo> httpEntity = new ArrayList<>();
//        List<SqlEntityInfo> sqlEntity = new ArrayList<>();
//        List<TlUser> fixUsers = new ArrayList<>();
//        TlUser y  = new TlUser();
//        y.setPassword("111111");
//        y.setUsername("admin");
//        fixUsers.add(y);
//        sqlEntity.add(sqlEntityInfo);
//        TlBootstrap bootstrap = new TlBootstrap();
//        bootstrap
//           .setServer(TlWebSocketServer.class)
//            .setDelay(1)
//           .setPort(18883)
//            .setAuth(true)
//            .setHttpEntity(httpEntity)
//            .setSqlEntity(sqlEntity)
//            .setFixUser(fixUsers)
//         //   存储配置
//            .setPublishService(new MemoryPublishServiceImpl())
//            .setRetainService(new MemoryRetainServiceImpl())
//            .setPubrelService(new MemoryPubrelServiceImpl())
//            .setSessionService(new MemorySessionServiceImpl())
//            .setDelay(10)
//           .setSsl(true)
//            .setCertPath("")
//            .setPrivatePath("")
//           .addAuthEntity(sqlEntityInfo)
//            .addAuthEntity(httpInfo)
//           .addAuthentication(new SqlTlAuthentication(()-> sqlEntity))
//            .addKafkaBridge(new TlKafkaInfo())
//             .addMysqlBridge(mySqlInfo)
//            .addBridgeEntity(new KafkaBridgeObserver())
//           .addAuthentication(new NoneA())
//            .start();
//    }
//
//    /**
//     * 获取配置的数据库连接配置 (模拟)
//     * @author hszhou
//     * @datetime: 2025-05-12 16:02:25
//     * @return List<SqlEntityInfo>
//     **/
//    public List<SqlEntityInfo> getConfigs() {
//        //认证的接口实例
//        List<SqlEntityInfo> entityInfos = new ArrayList<>();
//
//        SqlEntityInfo sqlEntityInfo = new SqlEntityInfo();
//        sqlEntityInfo.setHost("127.0.0.1");
//        sqlEntityInfo.setPort("3306");
//        sqlEntityInfo.setUsername("root");
//        sqlEntityInfo.setPassword("kangni");
//        sqlEntityInfo.setDatabase("watson");
//        sqlEntityInfo.setTable("sys_user");
//        sqlEntityInfo.setUsernameColumn("username");
//        sqlEntityInfo.setPasswordColumn("password");
//        sqlEntityInfo.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        entityInfos.add(sqlEntityInfo);
//
//
//        return entityInfos;
//    }
//
//    /**
//     * 获取kafka的配置 实际上是由用户配置
//     * @author hszhou
//     * @datetime: 2025-05-12 18:40:16
//     * @return List<TlKafkaInfo>
//     **/
//    //    private List<TlKafkaInfo>  getEntityInfo(){
//    //
//    //
//    //        entityInfos.add(new TlKafkaInfo("ws","172.28.33.102:9092","org.apache.kafka.common.serialization.StringSerializer","org.apache.kafka.common.serialization.StringSerializer"));
//    //        entityInfos.add(new TlKafkaInfo("ws","172.28.33.102:9092","org.apache.kafka.common.serialization.StringSerializer","org.apache.kafka.common.serialization.StringSerializer"));
//    //        return entityInfos;
//    //    }
//    public List<HttpEntityInfo> getConfigs(String username, String password) {
//        //认证的接口实例
//        List<HttpEntityInfo> configs = new ArrayList<>();
//
//
//
//        HashMap<String, String> headers = new HashMap<>(16);
//        headers.put("Content-Type", "application/json");
//
//
//        HashMap<String, String> headers2 = new HashMap<>(16);
//        headers2.put("Content-Type", "application/x-www-form-urlencoded");
//
//        HttpEntityInfo config = new HttpEntityInfo();
//        config.setUrl("http://127.0.0.1:8097/mqtt/login/test");
//
//        config.setMethod(POST);
//        config.setHeaders(headers2);
//        configs.add(config);
//
//        HttpEntityInfo config2 = new HttpEntityInfo();
//        config2.setUrl("http://127.0.0.1:8097/mqtt/login");
//        config2.setMethod(POST);
//        config2.setHeaders(headers);
//        configs.add(config2);
//
//        HttpEntityInfo config3 = new HttpEntityInfo();
//        config3.setUrl("http://127.0.0.1:8097/mqtt/login/test");
//        config3.setMethod(GET);
//        config3.setHeaders(new HashMap<>(1));
//        configs.add(config3);
//        return configs;
//    }
//
//}
