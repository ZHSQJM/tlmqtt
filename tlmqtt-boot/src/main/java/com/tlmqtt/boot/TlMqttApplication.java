package com.tlmqtt.boot;

import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.sql.SqlEntityInfo;
import com.tlmqtt.boot.authentication.http.AuthenticationHttpProvider;
import com.tlmqtt.boot.authentication.mysql.AuthenticationMysqlProvider;
import com.tlmqtt.boot.bridge.HttpBridge;
import com.tlmqtt.boot.bridge.kafka.KafkaProvider;
import com.tlmqtt.boot.bridge.mysql.MysqlProvider;
import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;
import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.core.TlBootstrap;
import com.tlmqtt.core.TlMqttServer;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Collections;

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

        HttpEntityInfo formLogin = AuthenticationHttpProvider.formLogin();
        HttpEntityInfo getLogin = AuthenticationHttpProvider.getLogin();
        HttpEntityInfo postLogin = AuthenticationHttpProvider.postLogin();

        SqlEntityInfo sqlEntityInfo = AuthenticationMysqlProvider.providerDemo();

        TlMySqlInfo mySqlInfo = MysqlProvider.mysqlInfo();
        TlKafkaInfo kafkaInfo = KafkaProvider.kafkaInfo();
        tlBootstrap.setServer(TlMqttServer.class)
            .setPort(18883)
            .setFixUser(Collections.singletonList(new TlUser("mqtt","mqtt")))
            .setHttpEntity(Arrays.asList(formLogin,getLogin,postLogin))
            .setSqlEntity(Collections.singletonList(sqlEntityInfo))
            .addBridgeMysql(mySqlInfo)
            .addBridgeKafka(kafkaInfo)
            .setSessionService(redisSessionService)
            .setPublishService(redisPublishService)
            .start();
    }
}
