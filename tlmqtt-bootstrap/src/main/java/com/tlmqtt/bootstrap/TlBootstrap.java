package com.tlmqtt.bootstrap;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.properties.TlBusinessProperties;
import com.tlmqtt.common.properties.TlMqttServerProperties;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.service.AliasService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hszhou
 */
@Slf4j
public class TlBootstrap {


    private TlMqttServer tlServer;
    private TlMqttServerProperties properties;
    private MqttComponentContainer.MqttComponentContainerBuilder containerBuilder;

    private boolean enableSocket = false;
    private boolean enableWebSocket = false;

    public TlBootstrap mqttServerProperties(TlMqttServerProperties props) {
        this.properties = props;
        this.containerBuilder = MqttComponentContainer.builder();
        this.containerBuilder.properties(props);

        return this;
    }

    public TlBootstrap sessionService(SessionService s) { containerBuilder.sessionService(s); return this; }
    public TlBootstrap publishService(PublishService s) { containerBuilder.publishService(s); return this; }
    public TlBootstrap pubrelService(PubrelService s) { containerBuilder.pubrelService(s); return this; }
    public TlBootstrap retainService(RetainService s) { containerBuilder.retainService(s); return this; }
    public TlBootstrap subscriptionService(SubscriptionService s) { containerBuilder.subscriptionService(s); return this; }
    public TlBootstrap shareSubscribeService(ShareSubscribeService s) { containerBuilder.shareSubscribeService(s); return this; }
    public TlBootstrap shareSubscribeClientChoose(IShareSubscribeClientChoose s) { containerBuilder.shareChoose(s); return this; }
    public TlBootstrap aliasService(AliasService s) { containerBuilder.aliasService(s); return this; }

    public TlBootstrap channelManager(ChannelManager channelManager){
        containerBuilder.channelManager(channelManager);
        return this;
    }
    public TlBootstrap retryManager(RetryManager retryManager){
        containerBuilder.retryManager(retryManager);
        return this;
    }
    public TlBootstrap authenticationManager(AuthenticationManager authenticationManager){
        containerBuilder.authenticationManager(authenticationManager);
        return this;
    }
    public TlBootstrap authorizationManager(AuthorizationManager authorizationManager){
        containerBuilder.authorizationManager(authorizationManager);
        return this;
    }
    public TlBootstrap mqttConfiguration(MqttConfiguration mqttConfiguration) {
        this.containerBuilder.mqttConfiguration(mqttConfiguration);
        return this;
    }

    public TlBootstrap socket() { this.enableSocket = true; return this; }
    public TlBootstrap websocket() { this.enableWebSocket = true; return this; }

    public TlBootstrap start() {
        // 构建容器
        containerBuilder.channelManager(new ChannelManager());
        TlBusinessProperties businessProperties = properties.getBusinessProperties();

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNamePrefix("demo-pool-%d").build();
        containerBuilder.executorService(new ThreadPoolExecutor(
            businessProperties.getCorePoolSize(),
            businessProperties.getMaxPoolSize(),
            businessProperties.getKeepAliveSeconds(),
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(businessProperties.getQueueCapacity()),namedThreadFactory));

        MqttComponentContainer container = containerBuilder.build();

        // 创建并设置服务器
        this.tlServer = new TlMqttServer(properties);
        this.tlServer.setup(container);

        if (enableSocket) {
            CompletableFuture.runAsync(() -> tlServer.startSocket(properties.getPortProperties().getMqtt()));
        }
        if (enableWebSocket) {
            CompletableFuture.runAsync(() -> tlServer.startWebsocket(properties.getPortProperties().getWebsocket()));
        }
        return this;
    }


}
