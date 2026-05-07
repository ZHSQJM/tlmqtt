package com.tlmqtt.bootstrap;

import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.interceptor.PublishInterceptor;
import com.tlmqtt.common.properties.TlMqttServerProperties;
import com.tlmqtt.core.alias.AliasService;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        this.containerBuilder.properties(props).ruleEngineDispatcher(new RuleEngineDispatcher());

        return this;
    }

    public TlBootstrap sessionService(SessionService s) { containerBuilder.sessionService(s); return this; }
    public TlBootstrap publishService(PublishService s) { containerBuilder.publishService(s); return this; }
    public TlBootstrap retainService(RetainService s) { containerBuilder.retainService(s); return this; }
    public TlBootstrap subscriptionService(SubscriptionService s) { containerBuilder.subscriptionService(s); return this; }
    public TlBootstrap shareSubscribeService(ShareSubscribeService s) { containerBuilder.shareSubscribeService(s); return this; }
    public TlBootstrap shareSubscribeClientChoose(IShareSubscribeClientChoose s) { containerBuilder.shareChoose(s); return this; }
    public TlBootstrap aliasService(AliasService s) { containerBuilder.aliasService(s); return this; }

    public TlBootstrap channelService(TlChannelService channelService){
        containerBuilder.channelService(channelService);
        return this;
    }
    public TlBootstrap schedulerTaskService(TlSchedulerTaskService s) {
        containerBuilder.schedulerTaskService(s);
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

    public TlBootstrap interceptors(List<PublishInterceptor> interceptors) {
        containerBuilder.interceptors(interceptors);
        return this;
    }
    public TlBootstrap mqttConfiguration(MqttConfiguration mqttConfiguration) {
        this.containerBuilder.mqttConfiguration(mqttConfiguration);
        return this;
    }

    public TlBootstrap executorService(ThreadPoolExecutor executorService) {
        containerBuilder.executorService(executorService);
        return this;
    }
    public TlBootstrap socket() { this.enableSocket = true; return this; }
    public TlBootstrap websocket() { this.enableWebSocket = true; return this; }

    public TlBootstrap start() {
        MqttComponentContainer container = containerBuilder.build();
        // 创建并设置服务器
        this.tlServer = new TlMqttServer(properties);
        this.tlServer.setup(container);
        if (enableSocket) {
            CompletableFuture.runAsync(() -> tlServer.startSocket(properties.getSsl().isEnabled()?properties.getPort().getSslMqtt():properties.getPort().getMqtt()));
        }
        if (enableWebSocket) {
            CompletableFuture.runAsync(() -> tlServer.startWebsocket(properties.getSsl().isEnabled()?properties.getPort().getSslWebsocket():properties.getPort().getWebsocket()));
        }
        return this;
    }


}
