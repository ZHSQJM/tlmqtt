package com.tlmqtt.boot;

import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.core.server.TlBootstrap;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        tlBootstrap
            //表示开启mqtt
            .socket()
            //表示开启websocket
            .websocket()
            .setFixUser(Collections.singletonList(new TlUser("admin","12345")))
            .setFixUser(Collections.singletonList(new TlUser("mqtt","mqtt")))
            .start();
    }
}
