package com.tlmqtt.core.handler;

import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PubReasonCode;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.interceptor.PublishInterceptor;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttPubAck;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;

import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.RetainService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * MQTT 消息发布处理器 (入站)
 * 负责处理客户端发送到 Broker 的 PUBLISH 报文
 * * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlPublishHandler extends AbstractTlHandler<TlMqttPublishReq> {

    private final ForwardMessageService forwardMessageService;

    public TlPublishHandler(RetainService retainService, AuthorizationManager authorizationManager,
        ForwardMessageService forwardMessageService, PublishService publishService,List<PublishInterceptor> interceptors) {
        this.forwardMessageService = forwardMessageService;
        this.publishService = publishService;
        super.setAuthorizationManager(authorizationManager);
        super.setRetainService(retainService);
        super.setInterceptors(interceptors);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPublishReq req,
        TlMqttSession session) {
        String clientId = session.getClientId();

        MqttVersion mqttVersion = session.getMqttVersion();
        TlMqttFixedHead fixedHead = req.getFixedHead();
        TlMqttPublishVariableHead variableHead = req.getVariableHead();
        Long messageId = variableHead.getMessageId();
        log.debug("【TLMQTT】Handling 【PUBACK】 event from client:【{}】, messageId: [{}]", clientId, messageId);
        MqttQoS messageQos = fixedHead.getQos();
        String topic = variableHead.getTopic();
        boolean retain = fixedHead.isRetain();
        // 1. ACL 发布权限校验
        if (!authorizationManager.checkPublishPermission(clientId, session.getUsername(), session.getIp(), topic)) {
            log.warn("【TLMQTT】ACL Deny: Client [{}] has no permission to publish to [{}]", clientId, topic);
            if (session.isVersion5()&& messageQos.value() > 0) {
                ctx.fireExceptionCaught(new TlMqttException(MqttErrorCode.UNAUTHORIZED, false, MqttMessageType.PUBLISH,messageId,
                    messageQos == MqttQoS.AT_LEAST_ONCE ? MqttMessageType.PUBACK : MqttMessageType.PUBREL));
            }
            return; // QoS 0 直接丢弃
        }

        // 3. 保留消息 (Retain) 处理
        if (retain) {
            log.debug("【TLMQTT】save Retain message from client:【{}】, topic: [{}]", clientId, topic);
            storeRetain(topic, req).subscribe();
        }
        // 4. 根据 QoS 分流处理核心逻辑
        switch (messageQos) {
            case AT_MOST_ONCE:
                log.debug("【TLMQTT】Publish message from client:【{}】, topic: [{}]", clientId, topic);
                forwardMessageService.publish(req, clientId, mqttVersion);
                break;

            case AT_LEAST_ONCE:
                // 先执行转发，确保消息送达订阅者
                forwardMessageService.publish(req, clientId, mqttVersion);
                // 再回复客户端 PUBACK
                sendAck(messageId, ctx.channel(), mqttVersion);
                break;

            case EXACTLY_ONCE:
                // QoS 2 必须先存入 Inbound 暂存区，回复 REC。
                // 真正的转发由 TlPubRelHandler 在收到客户端释放信号后再触发。
                handleQoS2Inbound(ctx, req, clientId, messageId, mqttVersion);
                break;

            default:
                log.warn("【TLMQTT】Unknown QoS level: [{}] from client [{}]", messageQos, clientId);
        }
    }
    /**
     * QoS 2 入站暂存处理
     */
    private void handleQoS2Inbound (ChannelHandlerContext ctx, TlMqttPublishReq req, String clientId, Long
    messageId, MqttVersion version){
        log.debug("【TLMQTT】Handling QoS 2 inbound message from client:【{}】, messageId: [{}]", clientId, messageId);
        // 关键：增加引用计数，防止 Netty 在异步存储完成前回收内存
        ReferenceCountUtil.retain(req);
        publishService.save(clientId, messageId, req)
            .subscribeOn(Schedulers.boundedElastic())
            .doFinally(signal -> {
                // 存储操作结束后释放引用
                ReferenceCountUtil.release(req);
            }).subscribe(v -> {
                // 存储成功，回复 PUBREC
                sendRec(messageId, ctx.channel(), version);
            }, e -> {
                log.error("【TLMQTT】Failed to persist QoS 2 inbound message for [{}], id [{}]", clientId, messageId, e);
                // 如果存储失败，通常不回 REC，客户端会因超时重发 PUBLISH
            });
    }
    /**
     * 保留消息持久化逻辑
     */
    private Mono<Boolean> storeRetain(String topic, TlMqttPublishReq req) {
        return Mono.defer(() -> {
            Object content = req.getPayload() != null ? req.getPayload().getContent() : null;
            // MQTT 规范：Payload 为空代表删除该主题的保留消息
            if (content == null || ("".equals(content))) {
                log.debug("【TLMQTT】Clearing retain message for topic: [{}]", topic);
                return retainService.clear(topic);
            } else {
                req.setAcceptTime(System.currentTimeMillis() / 1000);
                return retainService.save(topic, req);
            }
        });
    }

    private void sendAck(Long messageId, Channel channel, MqttVersion mqttVersion) {
        TlMqttPubAck res = TlMqttPubAck.build(messageId, PubReasonCode.SUCCESS.getCode(), null, null, mqttVersion);
        channel.writeAndFlush(res).addListener(future -> {
            if(future.isSuccess()){
                log.debug("【TLMQTT】Sent PUBACK to client:【{}】, messageId: [{}]", channel.id(), messageId);
            }
        });
    }

    private void sendRec(Long messageId, Channel channel, MqttVersion mqttVersion) {
        TlMqttPubRecReq res = TlMqttPubRecReq.build(messageId, PubReasonCode.SUCCESS.getCode(), null, null, mqttVersion);
        channel.writeAndFlush(res);
    }
}
