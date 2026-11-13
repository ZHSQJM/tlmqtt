package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.core.service.ForwardMessageService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlPubCompHandler extends AbstractTlHandler<TlMqttPubCompReq> {

    private final ForwardMessageService forwardMessageService;

    public TlPubCompHandler(ForwardMessageService forwardMessageService) {
        this.forwardMessageService = forwardMessageService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubCompReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        // 强制转换为 int 以适配 MessageIdManager 的 16 位逻辑
        int messageId = req.getVariableHead().getMessageId().intValue();
        log.debug("【TLMQTT】Handling 【PUBCOMP】 event from client:【{}】, messageId: [{}]", clientId, messageId);
        // handleAck 内部会完成 cancel + clearPubrel + 释放窗口 + 触发队列
        forwardMessageService.handleAck(clientId, Constant.PUBREL, messageId);
    }
}