package com.tlmqtt.bootstrap;

import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.interceptor.PublishInterceptor;
import com.tlmqtt.common.properties.TlMqttServerProperties;
import com.tlmqtt.common.properties.TlSessionProperties;
import com.tlmqtt.core.channel.TlChannelService;
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
import com.tlmqtt.core.alias.AliasService;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
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
    private final RetainService retainService;
    private final SubscriptionService subscriptionService;
    private final ShareSubscribeService shareSubscribeService;
    private final AliasService aliasService;
    private final IShareSubscribeClientChoose shareChoose;
    private final TlSchedulerTaskService schedulerTaskService;
    private final ExecutorService executorService;
    private final TlChannelService channelService;
    private final AuthenticationManager authenticationManager;
    private final AuthorizationManager authorizationManager;
    private final MqttConfiguration mqttConfiguration;

    private final List<PublishInterceptor> interceptors;

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
    private RuleEngineDispatcher ruleEngineDispatcher;

    public void initHandlers() {
        TlSessionProperties sessionProperties = properties.getSession();

        ForwardMessageService forwardService = new ForwardMessageService(aliasService, shareSubscribeService, shareChoose,
            subscriptionService, sessionService, publishService, channelService, schedulerTaskService, sessionProperties.getDelay(), sessionProperties.getMaxRetry());

        // 实例化 Handler
        this.exceptionHandler = new TlExceptionHandler(publishService, channelService, sessionService,mqttConfiguration,subscriptionService,forwardService,schedulerTaskService,ruleEngineDispatcher,sessionProperties.getTimeout());
        this.connectHandler = new TlConnectHandler(sessionService, publishService, retainService,
            channelService, authenticationManager,mqttConfiguration,schedulerTaskService,forwardService,ruleEngineDispatcher);
        this.disconnectHandler = new TlDisconnectHandler();
        this.heartBeatHandler = new TlHeartBeatHandler();
        this.pubAckHandler = new TlPubAckHandler(publishService, forwardService);
        this.pubCompHandler = new TlPubCompHandler(forwardService);
        this.publishHandler = new TlPublishHandler(retainService, authorizationManager, forwardService, publishService,interceptors);
        this.pubRecHandler = new TlPubRecHandler(publishService, forwardService);
        this.pubRelHandler = new TlPubRelHandler(forwardService, publishService);
        this.subscribeHandler = new TlSubscribeHandler(forwardService, authorizationManager, shareSubscribeService, sessionService, retainService, publishService, subscriptionService);
        this.unSubscribeHandler = new TlUnSubscribeHandler(subscriptionService);
    }
}
