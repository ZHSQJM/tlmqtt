package com.tlmqtt.core.server;

import com.tlmqtt.auth.AbstractTlAuthentication;
import com.tlmqtt.auth.AuthenticationManager;
import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.sql.SqlEntityInfo;
import com.tlmqtt.bridge.TlBridgeManager;
import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.config.TlMqttProperties;
import com.tlmqtt.common.config.TlPortProperties;
import com.tlmqtt.common.config.TlSslProperties;
import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.store.service.*;
import com.tlmqtt.store.service.impl.DefaultSubscriptionServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 15:00
 * @Description: 启动辅助类
 */
@Slf4j
public class TlBootstrap {


    private final TlServer tlServer;

    private boolean enableSocket;

    private boolean enableWebsocket;

    private int port;

    private int webSocketPort;

    public TlBootstrap() {

        MqttConfiguration mqttConfiguration = new MqttConfiguration();
        TlMqttProperties mqttProperties = mqttConfiguration.getMqttProperties();
        TlPortProperties portProperties = mqttProperties.getPort();

        TlSslProperties sslProperties = mqttProperties.getSsl();
        boolean ssl = sslProperties.isEnabled();
        this.port = portProperties.getMqtt();
        this.webSocketPort = portProperties.getWebsocket();
        if (ssl) {
            this.port = portProperties.getSslMqtt();
            this.webSocketPort = portProperties.getSslWebsocket();
            setCertPath(sslProperties.getCertPath());
            setPrivatePath(sslProperties.getPrivatePath());
        }
        this.enableSocket = false;
        this.enableWebsocket = false;
        this.tlServer = new TlServer();
    }




    /**
     * 是否开启认证
     * @author hszhou
     * @datetime: 2025-05-21 13:19:40
     * @param auth 是否开启认证
     * @return TlBootstrap
     **/
    public TlBootstrap setAuth(Boolean auth){

        tlServer.getAuthenticationManager().setAuth(auth);
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
        tlServer.getAuthenticationManager().addHttpEntity(()->httpEntityInfos);
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
        tlServer.getAuthenticationManager().addSqlEntity(()->sqlEntityInfos);
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
        tlServer.getAuthenticationManager().addFixUsers(fixUsers);
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
        tlServer.getStoreManager().setPublishService(publishService);
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
        tlServer.getStoreManager().setPubrelService(pubrelService);
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
        tlServer.getStoreManager().setRetainService(retainService);
        return this;
    }

    public TlBootstrap setSessionService(SessionService sessionService) {
        tlServer.getStoreManager().setSessionService(sessionService);
        tlServer.getStoreManager().setSubscriptionService(new DefaultSubscriptionServiceImpl(sessionService));
      return this;
    }

    public TlBootstrap setDelay(int delay){
        tlServer.getRetryManager().setDelay(delay);
        return this;
    }

    public TlBootstrap setSsl(boolean b) {
        tlServer.setSsl(b);
        return this;
    }

    public TlBootstrap setCertPath(String certPath ) {
        tlServer.setCertPath(certPath);
        return this;
    }

    public TlBootstrap setPrivatePath(String privatePath) {
        tlServer.setCertPath(privatePath);
        return this;
    }

    public TlBootstrap addAuthEntity(Object object) {
        tlServer.getAuthenticationManager().add(object);
        return this;
    }


    public TlBootstrap addBridgeKafka(TlKafkaInfo kafkaInfo) {
        tlServer.getBridgeManager().addKafkaInfo(kafkaInfo);
        return this;
    }
    public TlBootstrap addBridgeMysql(TlMySqlInfo tlMySqlInfo) {
        tlServer.getBridgeManager().addMysqlInfo(tlMySqlInfo);
        return this;
    }

    public TlBootstrap addAuthentication(AbstractTlAuthentication authentication) {
        tlServer.getAuthenticationManager().addAuthentication(authentication);
        return this;
    }

    public TlStoreManager getStoreManager(){
        return tlServer.getStoreManager();
    }

    public TlBridgeManager getBridgeManager(){
        return tlServer.getBridgeManager();
    }

    public AuthenticationManager getAuthenticationManager(){
       return tlServer.getAuthenticationManager();
    }

    public void start(){

        if (!enableSocket && !enableWebsocket) {
            throw new IllegalStateException("At least one service type needs to be enabled");
        }
        if(this.enableSocket){
            CompletableFuture.runAsync(()->  tlServer.startSocket(port));
        }
        if(this.enableWebsocket){
            CompletableFuture.runAsync(()->  tlServer.startWebsocket(webSocketPort));
        }
    }


    public TlBootstrap socket() {

        if(this.enableSocket){
            throw new IllegalStateException("The socket is already configured");
        }
        this.enableSocket = true;
        return this;
    }
    public TlBootstrap socket(int port) {

        this.port = port;
        socket();
        return this;
    }
    public TlBootstrap websocket() {
        if(this.enableWebsocket){
            throw new IllegalStateException("The webSocket is already configured");
        }
        this.enableWebsocket = true;
        return this;
    }
    public TlBootstrap websocket(int port) {
        this.webSocketPort = port;
        websocket();
        return this;
    }


}
