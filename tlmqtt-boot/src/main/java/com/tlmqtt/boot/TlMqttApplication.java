package com.tlmqtt.boot;

import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.http.HttpTlAuthentication;
import com.tlmqtt.auth.sql.SqlEntityInfo;
import com.tlmqtt.boot.bridge.HttpBridge;
import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;
import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.core.TlBootstrap;
import com.tlmqtt.core.TlMqttServer;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * @Author: hszhou
 * @Date: 2025/5/22 8:49
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@SpringBootApplication
@RequiredArgsConstructor
public class TlMqttApplication implements CommandLineRunner {



    private final SessionService redisSessionService;

    private final PublishService redisPublishService;

    private final TlBootstrap tlBootstrap;

    public static void main(String[] args) {

         SpringApplication.run(TlMqttApplication.class);

    }



    @Override
    public void run(String... args) throws Exception {
//        HashMap<String, String> headers = new HashMap<>(16);
//        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        HashMap<String, String> headers = new HashMap<>(16);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        HttpEntityInfo config = new HttpEntityInfo();
        config.setUrl("http://127.0.0.1:8097/mqtt/login/test");
        config.setMethod(HttpPost.METHOD_NAME);
        config.setHeaders(headers);



        HashMap<String, String> headers1 = new HashMap<>(16);
        headers1.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        HttpEntityInfo config1 = new HttpEntityInfo();
        config1.setUrl("http://127.0.0.1:8097/mqtt/login");
        config1.setMethod(HttpPost.METHOD_NAME);
        config1.setHeaders(headers1);
        HashMap<String, String> params = new HashMap<>(1);
        params.put(HttpTlAuthentication.USERNAME,"username");
        params.put(HttpTlAuthentication.PASSWORD,"password");
        config1.setParams(params);


        HttpEntityInfo config2 = new HttpEntityInfo();
        config2.setUrl("http://127.0.0.1:8097/mqtt/login/test");
        config2.setMethod(HttpGet.METHOD_NAME);
        config2.setHeaders(headers);


                TlMySqlInfo mySqlInfo = new TlMySqlInfo();
                mySqlInfo.setHost("127.0.0.1");
                mySqlInfo.setPort(3306);
                mySqlInfo.setUsername("root");
                mySqlInfo.setPassword("kangni");
                mySqlInfo.setDatabase("watson");
                mySqlInfo.setTable("mqtt_msg");

                mySqlInfo.setDriverClassName("com.mysql.cj.jdbc.Driver");
        TlKafkaInfo kafkaInfo = new TlKafkaInfo("ws", "172.28.33.102:9092",
            "org.apache.kafka.common.serialization.StringSerializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        ArrayList<HttpEntityInfo> httpEntityInfos = new ArrayList<>();
        tlBootstrap.setServer(TlMqttServer.class)
            .setAuth(false)
            .setFixUser(Collections.singletonList(new TlUser("mqtt","mqtt")))
            .setSqlEntity(new ArrayList<SqlEntityInfo>())
            .addBridgeMysql(mySqlInfo)
            .addBridgeKafka(kafkaInfo)
           .addHandler(new HttpBridge())
//            .setHttpEntity(Collections.singletonList(config))
//            .setHttpEntity(Collections.singletonList(config1))
//            .setHttpEntity(Collections.singletonList(config2))
          //  .setSessionService(redisSessionService)
            //.setPublishService(redisPublishService)
            .start();
    }
}
