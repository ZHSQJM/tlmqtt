package com.tlmqtt.boot.controller;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.entity.PubrelMessage;
import com.tlmqtt.core.TlBootstrap;
import com.tlmqtt.core.TlStoreManager;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.SessionService;
import com.tlmqtt.store.service.SubscriptionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @Author: hszhou
 * @Date: 2025/5/27 11:08
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Slf4j
public class MqttController {


    private final TlBootstrap tlBootstrap;
    @GetMapping("/detail")
    public HashMap<String, HashMap<String, Object>> detail() {
        log.info("detail");
        HashMap<String, HashMap<String, Object>> rs = new HashMap<>();
        TlStoreManager storeManager = tlBootstrap.getStoreManager();

        // 同步获取所有会话
        List<TlMqttSession> sessions = storeManager.getSessionService()
            .findAll()
            .collectList()
            .block(Duration.ofSeconds(5));
        log.info("返回session的值【{}】",sessions.size());

        HashMap<String, Object> clients = new HashMap<>();
        HashMap<String, Object> pubs = new HashMap<>();
        HashMap<String, Object> rel = new HashMap<>();

        sessions.forEach(session -> {
            String clientId = session.getClientId();

            log.info("获取clientId的数据【{}】",clientId);
            // 同步获取关联数据
            List<PublishMessage> pubMessages = storeManager.getPublishService()
                .findAll(clientId)
                .collectList()
                .block();
            log.info("返回pubMessages的值【{}】",pubMessages.size());
            List<PubrelMessage> pubrelMessages = storeManager.getPubrelService()
                .findAll(clientId)
                .collectList()
                .block();
            log.info("返回pubMessages的值【{}】",pubMessages.size());
            // 填充数据结构
            clients.put(clientId, session);
            pubs.put(clientId, pubMessages);
            rel.put(clientId, pubrelMessages);
        });

        rs.put("client", clients);
        rs.put("publish", pubs);
        rs.put("pubrel", rel);
        return rs;
    }
}
