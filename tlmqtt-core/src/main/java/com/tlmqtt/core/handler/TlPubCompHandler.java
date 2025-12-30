package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PubrelService;
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



    public TlPubCompHandler(ForwardMessageService forwardMessageService,
        PubrelService pubrelService ) {
        this.forwardMessageService = forwardMessageService;

        super.setPubrelService(pubrelService);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubCompReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        // 强制转换为 int 以适配 MessageIdManager 的 16 位逻辑
        int messageId = req.getVariableHead().getMessageId().intValue();
        // 2. 取消调度并清理持久化数据
        forwardMessageService.cancel(clientId, Constant.PUBREL, messageId)

            // 3. 维护窗口和 ID 释放 (复用 ForwardMessageService 的 handleAck 逻辑)
            .doOnSuccess(v -> forwardMessageService.handleAck(clientId,Constant.PUBREL, messageId))
            .subscribe();
    }
}