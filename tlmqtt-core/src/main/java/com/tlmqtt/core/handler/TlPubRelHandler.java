package com.tlmqtt.core.handler;

import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PubReasonCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PublishService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPubRelHandler extends AbstractTlHandler<TlMqttPubRelReq> {

    private final ForwardMessageService forwardMessageService;
    private final PublishService publishService;

    public TlPubRelHandler(ForwardMessageService forwardMessageService, PublishService publishService) {
        this.forwardMessageService = forwardMessageService;
        this.publishService = publishService;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPubRelReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        MqttVersion mqttVersion = session.getMqttVersion();
        long messageId = req.getVariableHead().getMessageId();

        log.debug("Received PUBREL from client: [{}], messageId: [{}]", clientId, messageId);

        // 1. 获取并移除暂存的消息 (防止重复转发)
        // 这个 publishReq 是在之前的 TlPublishHandler (QoS 2) 中存入 pubrelService 的
        publishService.find(clientId, messageId)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(publishReq -> {
                log.debug("Client [{}] QoS 2 message [{}] ready to forward", clientId, messageId);

                // 2. 触发分发流程：将这条消息发给所有订阅了该 Topic 的人
                // 注意：这里转发的是 publishReq（包含原始 Topic, Payload 等）
                return forwardMessageService.publish(publishReq, clientId, mqttVersion)
                    .then(publishService.clear(clientId, messageId))
                    .thenReturn(true);
            })
            // 如果找不到消息，说明可能是客户端重发的 PUBREL
            .defaultIfEmpty(false)
            .doFinally(signal -> {
                // 3. 无论转发是否成功，只要收到了 PUBREL，就必须回复 PUBCOMP
                // 否则客户端会一直重发 PUBREL 报文
                sendComp(messageId, ctx, mqttVersion);
            })
            .subscribe(isForwarded -> {
                if (Boolean.TRUE.equals(isForwarded)) {
                    log.debug("QoS 2 workflow finished for client [{}], id [{}]", clientId, messageId);
                } else {
                    // 这种情况通常发生在客户端重发了 PUBREL，而服务端之前已经转发过且删除了记录
                    log.warn("PUBREL received for client [{}] but no pending message found for id [{}]. (Duplicate?)", clientId, messageId);
                }
            });
    }

    private void sendComp(long messageId, ChannelHandlerContext ctx, MqttVersion mqttVersion) {
        // 构建 PUBCOMP 报文
        TlMqttPubCompReq res = TlMqttPubCompReq.build(messageId, PubReasonCode.SUCCESS.getCode(), null, null, mqttVersion);

        // 建议使用 writeAndFlush 直接写回通道
        ctx.writeAndFlush(res);
    }

}