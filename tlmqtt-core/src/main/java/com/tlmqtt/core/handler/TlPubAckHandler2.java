//package com.tlmqtt.core.handler;
//
//import com.tlmqtt.common.model.TlMqttSession;
//import com.tlmqtt.common.model.request.TlMqttPubAckReq;
//import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
//import com.tlmqtt.core.manager.RetryManager;
//import com.tlmqtt.core.service.ForwardMessageService;
//import com.tlmqtt.store.service.PublishService;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.scheduler.Schedulers;
//
///**
// * @author hszhou
// */
//@Slf4j
//@ChannelHandler.Sharable
//public class TlPubAckHandler2 extends AbstractTlHandler<TlMqttPubAckReq> {
//
//    private final ForwardMessageService forwardMessageService;
//
//    public TlPubAckHandler2(PublishService publishService,RetryManager retryManager, ForwardMessageService forwardMessageService) {
//       super.setPublishService(publishService);
//       super.setRetryManager(retryManager);
//       this.forwardMessageService = forwardMessageService;
//    }
//
//    @Override
//    public void handle(ChannelHandlerContext ctx, TlMqttPubAckReq req, TlMqttSession session) {
//        String clientId = session.getClientId();
//        log.debug("Handling 【PubAck】 event from client:【{}】", clientId);
//        TlMqttPubAckVariableHead variableHead = req.getVariableHead();
//        Long messageId = variableHead.getMessageId();
//        retryManager.cancelPublishRetry(messageId);
//      //  forwardMessageService.ack(clientId);
//        publishService
//            .clear(clientId, messageId)
//            .subscribeOn(Schedulers.boundedElastic())
//            .subscribe();
//    }
//}