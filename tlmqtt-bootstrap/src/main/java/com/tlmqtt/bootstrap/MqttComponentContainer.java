package com.tlmqtt.bootstrap;

import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.properties.TlAuthProperties;
import com.tlmqtt.common.properties.TlMqttServerProperties;
import com.tlmqtt.common.properties.TlSessionProperties;
import com.tlmqtt.core.handler.TlConnectHandler;
import com.tlmqtt.core.handler.TlDisconnectHandler;
import com.tlmqtt.core.handler.TlExceptionHandler;
import com.tlmqtt.core.handler.TlHeartBeatHandler;
import com.tlmqtt.core.handler.TlPubAckHandler;
import com.tlmqtt.core.handler.TlPubCompHandler;
import com.tlmqtt.core.handler.TlPubRecHandler;
import com.tlmqtt.core.handler.TlPubRelHandler;
import com.tlmqtt.core.handler.TlPublishHandler;
import com.tlmqtt.core.handler.TlSubscribeHandler;
import com.tlmqtt.core.handler.TlUnSubscribeHandler;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.service.AliasService;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.ExecutorService;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Builder
@Getter
public class MqttComponentContainer {

    private final TlMqttServerProperties properties;
    private final SessionService sessionService;
    private final PublishService publishService;
    private final PubrelService pubrelService;
    private final RetainService retainService;
    private final SubscriptionService subscriptionService;
    private final ShareSubscribeService shareSubscribeService;
    private final AliasService aliasService;
    private final IShareSubscribeClientChoose shareChoose;
    private final ExecutorService executorService;
    private final ChannelManager channelManager;
    private final RetryManager retryManager;
    private final AuthenticationManager authenticationManager;
    private final AuthorizationManager authorizationManager;
    private final MqttConfiguration mqttConfiguration;


    private TlConnectHandler connectHandler;
    private TlDisconnectHandler disconnectHandler;
    private TlHeartBeatHandler heartBeatHandler;
    private TlPubAckHandler pubAckHandler;
    private TlPubCompHandler pubCompHandler;
    private TlPublishHandler publishHandler;
    private TlPubRecHandler pubRecHandler;
    private TlPubRelHandler pubRelHandler;
    private TlSubscribeHandler subscribeHandler;
    private TlUnSubscribeHandler unSubscribeHandler;
    private TlExceptionHandler exceptionHandler;

    public void initHandlers() {


        ForwardMessageService forwardService = new ForwardMessageService(
            aliasService, shareSubscribeService, shareChoose,
            subscriptionService, sessionService, publishService, channelManager, retryManager);

        // 实例化 Handler
        this.exceptionHandler = new TlExceptionHandler(publishService, channelManager, sessionService,mqttConfiguration);
        this.connectHandler = new TlConnectHandler(sessionService, publishService, pubrelService, retainService, channelManager, authenticationManager, retryManager,mqttConfiguration);
        this.disconnectHandler = new TlDisconnectHandler();
        this.heartBeatHandler = new TlHeartBeatHandler();
        this.pubAckHandler = new TlPubAckHandler(publishService, retryManager, forwardService);
        this.pubCompHandler = new TlPubCompHandler(forwardService, retryManager, pubrelService);
        this.publishHandler = new TlPublishHandler(retainService, authorizationManager, forwardService, publishService);
        this.pubRecHandler = new TlPubRecHandler(publishService, pubrelService, retryManager);
        this.pubRelHandler = new TlPubRelHandler(forwardService, publishService);
        this.subscribeHandler = new TlSubscribeHandler(forwardService, authorizationManager, shareSubscribeService, sessionService, retainService, publishService, subscriptionService);
        this.unSubscribeHandler = new TlUnSubscribeHandler(subscriptionService);
    }
}
