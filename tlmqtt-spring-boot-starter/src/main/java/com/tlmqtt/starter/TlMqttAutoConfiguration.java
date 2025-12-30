package com.tlmqtt.starter;

import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.bootstrap.TlBootstrap;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.interceptor.PublishInterceptor;
import com.tlmqtt.common.properties.TlAuthProperties;
import com.tlmqtt.common.properties.TlSessionProperties;
import com.tlmqtt.core.alias.AliasService;
import com.tlmqtt.core.alias.DefaultAliasServiceImpl;
import com.tlmqtt.core.channel.DefaultChannelServiceImpl;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.core.share.IShareSubscribeClientChoose;
import com.tlmqtt.core.share.RandomSubscribeClientChoose;
import com.tlmqtt.core.task.HashedWheelTimerTlSchedulerTaskServiceImpl;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.impl.DefaultPublishServiceImpl;
import com.tlmqtt.store.service.impl.DefaultPubrelServiceImpl;
import com.tlmqtt.store.service.impl.DefaultRetainServiceImpl;
import com.tlmqtt.store.service.impl.DefaultShareSubscribeServiceImpl;
import com.tlmqtt.store.service.impl.DefaultSubscriptionServiceImpl;
import com.tlmqtt.store.service.session.DefaultSessionServiceImpl;
import com.tlmqtt.store.service.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(TlMqttProperties.class)
public class TlMqttAutoConfiguration {

    @Autowired(required = false)
    private List<PublishInterceptor> interceptors;

    @Bean
    public TlBootstrap bootstrap(@Autowired TlMqttProperties mqttProperties,
        @Autowired SessionService sessionService,
        @Autowired PublishService publishService,
        @Autowired PubrelService pubrelService,
        @Autowired RetainService retainService,
        @Autowired SubscriptionService subscriptionService,
        @Autowired IShareSubscribeClientChoose shareSubscribeClientChoose,
        @Autowired ShareSubscribeService shareSubscribeService,
        @Autowired AliasService aliasService,
        @Autowired TlSchedulerTaskService schedulerTaskService,
        @Autowired TlChannelService channelService,
        @Autowired AuthenticationManager authenticationManager,
        @Autowired AuthorizationManager authorizationManager,
        @Autowired MqttConfiguration mqttConfiguration) {

        TlBootstrap bootstrap = new TlBootstrap();
        sessionService.addListener(publishService);
        sessionService.addListener(pubrelService);
        sessionService.addListener(retainService);
        sessionService.addListener(subscriptionService);
        sessionService.addListener(shareSubscribeService);
       return bootstrap.mqttServerProperties(mqttProperties)
                 .socket()
                 .websocket()
                 .sessionService(sessionService)
                 .publishService(publishService)
                 .pubrelService(pubrelService)
                 .retainService(retainService)
                 .subscriptionService(subscriptionService)
                 .shareSubscribeClientChoose(shareSubscribeClientChoose)
                 .shareSubscribeService(shareSubscribeService)
                 .aliasService(aliasService)
                 .schedulerTaskService(schedulerTaskService)
                 .channelService(channelService)
                 .authenticationManager(authenticationManager)
                 .authorizationManager(authorizationManager)
                 .mqttConfiguration(mqttConfiguration)
           .interceptors(interceptors)
                 .start();
    }

    @Bean
    public TlSessionProperties sessionProperties(@Autowired TlMqttProperties mqttProperties){
        return mqttProperties.getSessionProperties();
    }

    @Bean
    public TlAuthProperties authProperties(@Autowired TlMqttProperties mqttProperties){
        return mqttProperties.getAuthProperties();
    }

    @ConditionalOnMissingBean(SessionService.class)
    @Bean
    public SessionService sessionService(){
        return new DefaultSessionServiceImpl();
    }

    @ConditionalOnMissingBean(PublishService.class)
    @Bean
    public PublishService publishService(){
        return new DefaultPublishServiceImpl();
    }

    @ConditionalOnMissingBean(PubrelService.class)
    @Bean
    public PubrelService pubrelService(){
        return new DefaultPubrelServiceImpl();
    }

    @ConditionalOnMissingBean(RetainService.class)
    @Bean
    public RetainService retainService(){
        return new DefaultRetainServiceImpl();
    }

    @ConditionalOnMissingBean(SubscriptionService.class)
    @ConditionalOnBean(SessionService.class)
    @Bean
    public SubscriptionService subscriptionService(@Autowired SessionService sessionService){
        return new DefaultSubscriptionServiceImpl(sessionService);
    }
    @ConditionalOnMissingBean(ShareSubscribeService.class)
    @Bean
    public ShareSubscribeService shareSubscribeService(){
        return new DefaultShareSubscribeServiceImpl();
    }


    @ConditionalOnMissingBean(IShareSubscribeClientChoose.class)
    @Bean
    public IShareSubscribeClientChoose shareSubscribeClientChoose(){
        return new RandomSubscribeClientChoose();
    }

    @ConditionalOnMissingBean(AliasService.class)
    @Bean
    public AliasService aliasService(){
        return new DefaultAliasServiceImpl();
    }

    @ConditionalOnMissingBean(TlSchedulerTaskService.class)
    @Bean
    public TlSchedulerTaskService schedulerTaskService(){
        return new HashedWheelTimerTlSchedulerTaskServiceImpl();
    }

    @ConditionalOnMissingBean(TlChannelService.class)
    @Bean
    public TlChannelService channelService(){
        return new DefaultChannelServiceImpl();
    }

    @Bean
    public AuthenticationManager authenticationManager(@Autowired TlAuthProperties authProperties){
        return new AuthenticationManager(authProperties.isEnabled(), authProperties.getUser());
    }

    @Bean
    public AuthorizationManager authorizationManager(){
        return new AuthorizationManager();
    }


    @Bean
    public MqttConfiguration mqttConfiguration(){
       return new MqttConfiguration();

    }
}
