package com.tlmqtt.core;

import com.lmax.disruptor.EventHandler;
import com.tlmqtt.auth.AbstractTlAuthentication;
import com.tlmqtt.auth.AuthenticationManager;
import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.sql.SqlEntityInfo;
import com.tlmqtt.bridge.TlBridgeManager;
import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.KafkaBridgeObserver;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.store.service.*;
import com.tlmqtt.store.service.impl.DefaultSubscriptionServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 15:00
 * @Description: 启动辅助类
 */
@Slf4j
public class TlBootstrap {


    private AbstractTlServer tlMqttServer;



    public TlBootstrap() {
    }



    /**
     * 设置启动端口
     * @author hszhou
     * @datetime: 2025-05-21 13:19:16
     * @param port 启动端口
     * @return TlBootstrap
     **/
    public TlBootstrap setPort(int port){
        tlMqttServer.setPort(port);
        return this;
    }

    /**
     * 是否开启认证
     * @author hszhou
     * @datetime: 2025-05-21 13:19:40
     * @param auth 是否开启认证
     * @return TlBootstrap
     **/
    public TlBootstrap setAuth(Boolean auth){

        tlMqttServer.getAuthenticationManager().setAuth(auth);
        return this;
    }

    /**
     * 设置http的接口认证
     * @author hszhou
     * @datetime: 2025-05-21 13:20:12
     * @param httpEntityInfos http请求认证
     * @return TlBootstrap
     **/
    public TlBootstrap setHttpEntity(List<HttpEntityInfo> httpEntityInfos) {
        tlMqttServer.getAuthenticationManager().addHttpEntity(()->httpEntityInfos);
        return this;
    }

    /**
     * 添加sql认证
     * @author hszhou
     * @datetime: 2025-05-21 13:20:41
     * @param sqlEntityInfos sql认证
     * @return TlBootstrap
     **/
    public TlBootstrap setSqlEntity(List<SqlEntityInfo> sqlEntityInfos) {
        tlMqttServer.getAuthenticationManager().addSqlEntity(()->sqlEntityInfos);
        return this;
    }

    /**
     * 添加固定用户认证
     * @author hszhou
     * @datetime: 2025-05-21 13:21:01
     * @param fixUsers 固定用户
     * @return TlBootstrap
     **/
    public TlBootstrap setFixUser(List<TlUser> fixUsers) {
        tlMqttServer.getAuthenticationManager().addFixUsers(fixUsers);
        return this;
    }


    /**
     * 设置publish的消息存储
     * @author hszhou
     * @datetime: 2025-05-21 13:21:35
     * @param publishService publishService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setPublishService(PublishService publishService) {
        tlMqttServer.getStoreManager().setPublishService(publishService);
        return this;
    }


    /**
     * 设置pubrelService的存储
     * @author hszhou
     * @datetime: 2025-05-21 13:22:13
     * @param pubrelService pubrelService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setPubrelService(PubrelService pubrelService) {
        tlMqttServer.getStoreManager().setPubrelService(pubrelService);
        return this;
    }

    /**
     *
     * @author hszhou
     * @datetime: 2025-05-21 13:22:54
     * @param retainService retainService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setRetainService(RetainService retainService) {
         tlMqttServer.getStoreManager().setRetainService(retainService);
        return this;
    }

    public TlBootstrap setSessionService(SessionService sessionService) {
      tlMqttServer.getStoreManager().setSessionService(sessionService);
      tlMqttServer.getStoreManager().setSubscriptionService(new DefaultSubscriptionServiceImpl(sessionService));
      return this;
    }



    public TlBootstrap setDelay(int delay){
        tlMqttServer.getStoreManager().getRetryService().setDelay(delay);
        return this;
    }

    public TlBootstrap setSsl(boolean b) {
        tlMqttServer.setSsl(b);
        return this;
    }

    public TlBootstrap setCertPath(String certPath ) {
        tlMqttServer.setCertPath(certPath);
        return this;
    }

    public TlBootstrap setPrivatePath(String privatePath) {
        tlMqttServer.setCertPath(privatePath);
        return this;
    }

    public TlBootstrap addAuthEntity(Object object) {
        tlMqttServer.getAuthenticationManager().add(object);
        return this;
    }


    public TlBootstrap addBridgeKafka(TlKafkaInfo kafkaInfo) {
        tlMqttServer.getBridgeManager().addKafkaInfo(kafkaInfo);
        return this;
    }
    public TlBootstrap addBridgeMysql(TlMySqlInfo tlMySqlInfo) {
        tlMqttServer.getBridgeManager().addMysqlInfo(tlMySqlInfo);
        return this;
    }
    public TlBootstrap addHandler(EventHandler<PublishMessage> handler) {

        tlMqttServer.getBridgeManager().addHandler(handler);
        return this;
    }

    public TlBootstrap addAuthentication(AbstractTlAuthentication authentication) {
        tlMqttServer.getAuthenticationManager().addAuthentication(authentication);
        return this;
    }


    public TlStoreManager getStoreManager(){
        return tlMqttServer.getStoreManager();
    }

    public TlBridgeManager getBridgeManager(){
        return tlMqttServer.getBridgeManager();
    }

    public AuthenticationManager getAuthenticationManager(){
       return tlMqttServer.getAuthenticationManager();
    }

    public void start(){
        tlMqttServer.start();
    }

    public TlBootstrap setServer(Class<? extends AbstractTlServer> clazz) {
        try {
            tlMqttServer = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("init fail",e);
        }
        return this;
    }


}
