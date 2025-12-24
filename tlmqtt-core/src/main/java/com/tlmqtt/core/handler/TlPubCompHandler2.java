//package com.tlmqtt.core.handler;
//
//import com.tlmqtt.common.model.TlMqttSession;
//import com.tlmqtt.common.model.request.TlMqttPubCompReq;
//import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
//import com.tlmqtt.core.manager.RetryManager;
//import com.tlmqtt.core.service.ForwardMessageService;
//import com.tlmqtt.store.service.PubrelService;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.scheduler.Schedulers;
//
///**
// * @author hszhou
// */
//@ChannelHandler.Sharable
//@Slf4j
//public class TlPubCompHandler2 extends AbstractTlHandler<TlMqttPubCompReq> {
//
//
//
//    private final ForwardMessageService forwardMessageService;
//
//    public TlPubCompHandler2(ForwardMessageService forwardMessageService, RetryManager retryManager, PubrelService pubrelService) {
//        this.forwardMessageService = forwardMessageService;
//        super.setRetryManager(retryManager);
//        super.setPubrelService(pubrelService);
//    }
//
//    @Override
//    public void handle(ChannelHandlerContext ctx, TlMqttPubCompReq req, TlMqttSession session) {
//
//        String clientId = session.getClientId();
//        log.debug("Handling 【PUBCOMP】 event from client:【{}】", clientId);
//        TlMqttPubCompVariableHead variableHead = req.getVariableHead();
//        Long messageId = variableHead.getMessageId();
//        retryManager.cancelPubrelRetry(messageId);
//        forwardMessageService.ack(clientId);
//        pubrelService
//            .clear(clientId, messageId)
//            .subscribeOn(Schedulers.boundedElastic())
//            .subscribe();
//    }
//}