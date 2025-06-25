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
 * @author hszhou
 */
@Slf4j
public class TlBootstrap {


    private final TlServer tlServer;

    private boolean enableSocket;

    private boolean enableWebsocket;

    private int port;

    private int webSocketPort;

    /**
     * 创建TlBootstrap
     **/
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
     * @param auth 是否开启认证
     * @return TlBootstrap
     **/
    public TlBootstrap setAuth(Boolean auth){

        tlServer.getAuthenticationManager().setAuth(auth);
        return this;
    }

    /**
     * 设置http的接口认证
     * @param httpEntityInfos http请求认证
     * @return TlBootstrap
     **/
    public TlBootstrap setHttpEntity(List<HttpEntityInfo> httpEntityInfos) {
        tlServer.getAuthenticationManager().addHttpEntity(()->httpEntityInfos);
        return this;
    }

    /**
     * 添加sql认证
     * @param sqlEntityInfos sql认证
     * @return TlBootstrap
     **/
    public TlBootstrap setSqlEntity(List<SqlEntityInfo> sqlEntityInfos) {
        tlServer.getAuthenticationManager().addSqlEntity(()->sqlEntityInfos);
        return this;
    }

    /**
     * 添加固定用户认证
     * @param fixUsers 固定用户
     * @return TlBootstrap
     **/
    public TlBootstrap setFixUser(List<TlUser> fixUsers) {
        tlServer.getAuthenticationManager().addFixUsers(fixUsers);
        return this;
    }


    /**
     * 设置publish的消息存储
     * @param publishService publishService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setPublishService(PublishService publishService) {
        tlServer.getStoreManager().setPublishService(publishService);
        return this;
    }


    /**
     * 设置pubrelService的存储
     * @param pubrelService pubrelService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setPubrelService(PubrelService pubrelService) {
        tlServer.getStoreManager().setPubrelService(pubrelService);
        return this;
    }

    /**
     *
     * @param retainService retainService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setRetainService(RetainService retainService) {
        tlServer.getStoreManager().setRetainService(retainService);
        return this;
    }

    /**
     * 设置sessionService的存储
     * @param sessionService sessionService的实现类
     * @return TlBootstrap
     **/
    public TlBootstrap setSessionService(SessionService sessionService) {
        tlServer.getStoreManager().setSessionService(sessionService);
        tlServer.getStoreManager().setSubscriptionService(new DefaultSubscriptionServiceImpl(sessionService));
      return this;
    }

    /**
     * 设置重试的延迟时间
     * @param delay 重试的延迟时间
     * @return TlBootstrap
     **/
    public TlBootstrap setDelay(int delay){
        tlServer.getRetryManager().setDelay(delay);
        return this;
    }

    /**
     * 添加ssl
     * @param b 是否开启ssl
     * @return TlBootstrap
     **/
    public TlBootstrap setSsl(boolean b) {
        tlServer.setSsl(b);
        return this;
    }

    /**
     * 添加ssl证书
     * @param certPath 证书路径
     * @return TlBootstrap
     **/
    public TlBootstrap setCertPath(String certPath ) {
        tlServer.setCertPath(certPath);
        return this;
    }

    /**
     * 添加ssl证书
     * @param privatePath 私钥路径
     * @return TlBootstrap
     **/
    public TlBootstrap setPrivatePath(String privatePath) {
        tlServer.setCertPath(privatePath);
        return this;
    }

    /**
     * 添加认证实体
     * @param object 认证实体
     * @return TlBootstrap
     **/
    public TlBootstrap addAuthEntity(Object object) {
        tlServer.getAuthenticationManager().add(object);
        return this;
    }


    /**
     * 添加桥接信息
     * @param kafkaInfo kafka信息
     * @return TlBootstrap
     **/
    public TlBootstrap addBridgeKafka(TlKafkaInfo kafkaInfo) {
        tlServer.getBridgeManager().addKafkaInfo(kafkaInfo);
        return this;
    }
    /**
     * 添加桥接信息
     * @param tlMySqlInfo mysql信息
     * @return TlBootstrap
     **/
    public TlBootstrap addBridgeMysql(TlMySqlInfo tlMySqlInfo) {
        tlServer.getBridgeManager().addMysqlInfo(tlMySqlInfo);
        return this;
    }

    /**
     * 添加认证信息
     * @param authentication 认证信息
     * @return TlBootstrap
     **/
    public TlBootstrap addAuthentication(AbstractTlAuthentication authentication) {
        tlServer.getAuthenticationManager().addAuthentication(authentication);
        return this;
    }

    /**
     * 获取存储管理器
     * @return TlStoreManager
     **/
    public TlStoreManager getStoreManager(){
        return tlServer.getStoreManager();
    }

    /**
     * 获取桥接管理器
     * @return TlBridgeManager
     **/
    public TlBridgeManager getBridgeManager(){
        return tlServer.getBridgeManager();
    }

    /**
     * 获取认证管理器
     * @return AuthenticationManager
     **/
    public AuthenticationManager getAuthenticationManager(){
       return tlServer.getAuthenticationManager();
    }

    /**
     * 启动服务
     **/
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


    /**
     * 添加socket服务
     * @return TlBootstrap
     **/
    public TlBootstrap socket() {

        if(this.enableSocket){
            throw new IllegalStateException("The socket is already configured");
        }
        this.enableSocket = true;
        return this;
    }
    /**
     * 添加socket服务
     * @param port 端口
     * @return TlBootstrap
     **/
    public TlBootstrap socket(int port) {

        this.port = port;
        return socket();
    }

    /**
     * 添加websocket服务
     * @return TlBootstrap
     **/
    public TlBootstrap websocket() {
        if(this.enableWebsocket){
            throw new IllegalStateException("The webSocket is already configured");
        }
        this.enableWebsocket = true;
        return this;
    }
    /**
     * 添加websocket服务
     * @param port 端口
     * @return TlBootstrap
     **/
    public TlBootstrap websocket(int port) {
        this.webSocketPort = port;
        return  websocket();
    }


}
