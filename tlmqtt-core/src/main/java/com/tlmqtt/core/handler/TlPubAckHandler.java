package com.tlmqtt.core.handler;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PublishService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPubAckHandler extends AbstractTlHandler<TlMqttPubAckReq> {
    private final ForwardMessageService forwardMessageService;


    public TlPubAckHandler(PublishService publishService, ForwardMessageService forwardMessageService) {
        super.setPublishService(publishService);

        this.forwardMessageService = forwardMessageService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubAckReq req, TlMqttSession session) {

        String clientId = session.getClientId();

        // MQTT 协议中 MessageId 为 16位无符号整型
        int messageId = req.getVariableHead().getMessageId().intValue();
        log.debug("【TLMQTT】Handling 【PUBACK】 event from client:【{}】, messageId: [{}]", clientId, messageId);
        // 1. 核心：通知 ForwardMessageService 完成确认
        // 该方法内部会：1.取消定时任务, 2. 释放 ID  3. 减小 In-Flight 计数  4. 触发队列中的下一条消息  // 2. 异步清理持久化的离线消息
        forwardMessageService.handleAck(clientId, Constant.PUBLISH,messageId);


    }
}