package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;

import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPubRecHandler extends AbstractTlHandler<TlMqttPubRecReq> {


    private final PubrelService pubrelService;


    private final ForwardMessageService forwardMessageService;

    public TlPubRecHandler(PublishService publishService, PubrelService pubrelService, ForwardMessageService forwardMessageService
         ) {
        super.setPublishService(publishService);
        this.forwardMessageService = forwardMessageService;
        this.pubrelService = pubrelService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubRecReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        long messageId = req.getVariableHead().getMessageId();

        log.debug("Received PUBREC from client: [{}], messageId: [{}]", clientId, messageId);

        // // 1. 停止 PUBLISH 重试
        TlMqttPubRelReq relReq = TlMqttPubRelReq.build(messageId, MqttErrorCode.SUCCESS.byteValue());
        forwardMessageService.cancel(clientId, Constant.PUBLISH, messageId)
            // 2. 清除 publish消息
            .then(publishService.clear(clientId, messageId))
            .then(pubrelService.save(clientId, messageId, relReq))
            // 3. 开启 PUBREL 重试
            .then(forwardMessageService.scheduleWithRetry(Constant.PUBREL, clientId, relReq, 1))
            .subscribe();


    }






}