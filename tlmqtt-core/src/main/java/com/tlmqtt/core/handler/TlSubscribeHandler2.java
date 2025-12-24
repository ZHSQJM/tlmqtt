//package com.tlmqtt.core.handler;
//
//import com.tlmqtt.auth.acl.AuthorizationManager;
//import com.tlmqtt.common.enums.MqttErrorCode;
//import com.tlmqtt.common.enums.MqttQoS;
//import com.tlmqtt.common.enums.MqttVersion;
//import com.tlmqtt.common.model.TlMqttSession;
//import com.tlmqtt.common.model.entity.TlSubClient;
//import com.tlmqtt.common.model.entity.TlTopic;
//import com.tlmqtt.common.model.request.TlMqttPublishReq;
//import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
//import com.tlmqtt.common.model.response.TlMqttSubAck;
//import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
//import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
//import com.tlmqtt.core.service.ForwardMessageService;
//import com.tlmqtt.store.service.PublishService;
//import com.tlmqtt.store.service.RetainService;
//import com.tlmqtt.store.service.ShareSubscribeService;
//import com.tlmqtt.store.service.SubscriptionService;
//import com.tlmqtt.store.service.session.SessionService;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * @author hszhou
// */
//@Slf4j
//@ChannelHandler.Sharable
//public class TlSubscribeHandler2 extends AbstractTlHandler<TlMqttSubscribeReq> {
//
//
//
//    protected final ForwardMessageService forwardMessageService;
//
//    public TlSubscribeHandler2(ForwardMessageService forwardMessageService, AuthorizationManager aclManager, ShareSubscribeService shareSubscribeService,
//         SessionService sessionService, RetainService retainService,
//        PublishService publishService, SubscriptionService subscriptionService) {
//        this.forwardMessageService = forwardMessageService;
//        super.setAclManager(aclManager);
//        super.setSessionService(sessionService);
//        super.setShareSubscribeService(shareSubscribeService);
//        super.setPublishService(publishService);
//        super.setRetainService(retainService);
//        super.setSubscriptionService(subscriptionService);
//    }
//
//    @Override
//    public void handle(ChannelHandlerContext ctx, TlMqttSubscribeReq req, TlMqttSession session) {
//        Channel channel = ctx.channel();
//        MqttVersion mqttVersion = session.getMqttVersion();
//        String clientId = session.getClientId();
//        log.debug("Handling 【SUBSCRIBE】 event from client:【{}】", clientId);
//
//        List<TlTopic> topics = req.getPayload().getTopics();
//        Set<TlTopic> successTopic = new HashSet<>();
//        int[] codes = new int[topics.size()];
//
//        TlMqttSubscribeVariableHead variableHead = req.getVariableHead();
//        int messageId = variableHead.getMessageId();
//
//
//        //发送订阅确认
//        for (int i = 0; i < topics.size(); i++) {
//            TlTopic tlTopic = topics.get(i);
//
//            String topicName = tlTopic.getName();
//
//            //todo 校验主题是否有效
//            if (aclManager.checkSubscribePermission(session, topicName)) {
//                codes[i] = tlTopic.getQos();
//                successTopic.add(tlTopic);
//            } else {
//                //这里有可能订阅多个主题 所有不能跑异常
//                codes[i] = MqttErrorCode.UNAUTHORIZED.byteValue();
//            }
//            log.info("【SUBSCRIBE】 event from client:【{}】--【{}】", clientId, topicName);
//        }
//        TlMqttSubAck res = TlMqttSubAck.build(codes, messageId,null,null);
//        channel.writeAndFlush(res).addListener(future -> {
//            if (future.isSuccess()) {
//                session.getTopics().addAll(successTopic.stream().map(TlTopic::getName).collect(Collectors.toSet()));
//                sessionService
//                    .save(session)
//                    .onErrorResume(e -> {
//                        // 3. 捕获异常并返回空流，防止进入 thenMany
//                        log.debug("Subscription aborted due to error: {}", e.getMessage());
//                        return Mono.empty();
//                    }).thenMany(Flux.fromIterable(successTopic).flatMap(topic -> {
//                        int qos= topic.getQos();
//                        Integer subscriptionIdentifier = variableHead.getSubscriptionIdentifier();
//                        Integer retainHandling = topic.getRetainHandling();
//                        TlSubClient client =  TlSubClient.builder()
//                            .qos(qos)
//                            .clientId(clientId)
//                            .topic(topic.getName())
//                            .mqttVersion(mqttVersion)
//                            .subscriptionIdentifier(subscriptionIdentifier)
//                            .isShared(topic.isShare())
//                            .retainAsPublished(topic.getRetainAsPublished())
//                            .noLocal(topic.getNoLocal())
//                            .group(topic.getGroup())
//                            .build();
//                        if(retainHandling != null  && retainHandling== 2){
//                            return Flux.empty();
//                        }
//
//
//                        Mono<Boolean> subscriptionOperation = client.getIsShared()
//                            ?  shareSubscribeService.subscribeShare(client)
//                            : subscriptionService.subscribe(client);
//
//                        //找到主题的保留消息
//                        return subscriptionOperation.thenMany(retainService.find(topic.getName()).doOnNext(publishReq -> {
//                            log.debug("Send retain message 【{}】 to client 【{}】", publishReq.toString(), clientId);
//                            TlMqttPublishVariableHead publishReqVariableHead = publishReq.getVariableHead();
//                            Integer messageExpiryInterval = publishReqVariableHead.getMessageExpiryInterval();
//                            if (MqttVersion.MQTT_5 == mqttVersion && messageExpiryInterval != null) {
//                                long currentTime = System.currentTimeMillis()/1000;
//                                Long acceptTime = publishReq.getAcceptTime();
//                                if (acceptTime + messageExpiryInterval < currentTime) {
//                                    log.debug("Retain message 【{}】 is expired", publishReq);
//                                    return;
//                                }
//                                long remainingTime = messageExpiryInterval - (currentTime - acceptTime);
//                                publishReqVariableHead.setMessageExpiryInterval((int) remainingTime);
//                            }
//                            //这是保留消息的qos等级
//                            int retainQos = publishReq.getFixedHead().getQos().value();
//                            int realQos = Math.min(qos, retainQos);
//                            MqttQoS mqttQoS = MqttQoS.valueOf(realQos);
//                            TlMqttPublishReq publishMessage = forwardMessageService.build(publishReq, mqttQoS,session,client);
//                            if(mqttQoS != MqttQoS.AT_MOST_ONCE){
//                               publishService.save(clientId, publishMessage.getVariableHead().getMessageId(), publishMessage).subscribe();
//                            }
//                            channel.writeAndFlush(publishMessage);
//                        }));
//                    })).subscribe();
//            }
//        });
//    }
//
//
//
//}