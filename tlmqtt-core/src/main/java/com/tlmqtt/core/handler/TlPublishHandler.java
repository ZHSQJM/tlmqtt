package com.tlmqtt.core.handler;

import com.tlmqtt.auth.acl.AclManager;
import com.tlmqtt.bridge.TlBridgeManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PubReasonCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttPubAck;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.manager.MessageManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlPublishHandler extends AbstractTlHandler<TlMqttPublishReq> {

    private final TlStoreManager storeManager;

    private final AclManager aclManager;

    private final TlBridgeManager bridgeManager;

    private final MessageManager messageService;



    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttPublishReq req, TlMqttSession session) {

        //log.info("收到的消息是【{}】-【{}】-【{}】-【{}】",req.hashCode(),req.getFixedHead().hashCode(),req.getVariableHead().hashCode(),req.getPayload().hashCode());
        String clientId = session.getClientId();

        Channel channel = ctx.channel();
        MqttVersion mqttVersion = session.getMqttVersion();
        //log.debug("Handling 【PUBLISH】 event from client:【{}】", clientId);
        ///log.info("收到客户端【{}】的消息【{}】",clientId,req);

        TlMqttFixedHead fixedHead = req.getFixedHead();
        TlMqttPublishVariableHead variableHead = req.getVariableHead();

        boolean retain = fixedHead.isRetain();
        MqttQoS messageQos = fixedHead.getQos();
        String topic = variableHead.getTopic();

        String username =session.getUsername();
        String ip = session.getIp();
        if (!aclManager.checkPublishPermission(clientId,username,ip, topic)) {
            log.error("Client 【{}】 no permission to publish topic 【{}】", clientId, topic);
            return;
        }
        Long messageId = variableHead.getMessageId();

        /*如果是保留消息 存储*/
        if (retain) {
            storeRetain(topic,req).subscribe();
        }


        switch (messageQos) {
            case AT_LEAST_ONCE:
                sendAck(messageId, channel,mqttVersion);
                break;
            case EXACTLY_ONCE:
                //这里需要保存消息 key是messageId，value是req，在收到rel消息后 需要将这个消息转发到其他订阅的客户端 在rel只能收到messageId，没有其他的信息
                channel.attr(AttributeKey.valueOf(Constant.PUB_MSG)).set(req);
                //发送rec消息给发送者
                sendRec(messageId, channel,mqttVersion);
                return;
            default:
        }
        //转发给其他的订阅的客户端


        messageService.publish(req,clientId,mqttVersion);
    }

    /**
     * @description: 在新订阅的时候发送
     * @author hszhou
     * 2025-04-29 18:29:08
     * @param:
     * @param: topic 主题
     * @param: content 内容
     * @param: qos
     * @param: messageId
     * @return: Mono<Boolean>
     **/
    private Mono<Boolean> storeRetain(String topic,TlMqttPublishReq req) {

        return Mono.defer(() -> {
            Object content = req.getPayload().getContent();
            if ("".equals(content) || null == content) {
                return storeManager.getRetainService().clear(topic);
            } else {
                return storeManager.getRetainService().save(topic, req);
            }
        });
    }

    /**
     * 构建ack消息发送返回
     * @param channel 消息ID
     * @param messageId channel
     */
    private void sendAck(Long messageId,Channel channel,MqttVersion mqttVersion) {

        /*
         * 0	0x00	成功	消息被接收。QoS为1的消息已发布。
         * 16	0x10	无匹配的订阅者	消息被接收，但没有订阅者。只有服务端会发送此原因码。如果服务端得知没有匹配的订阅者，服务端可以使用此原因码代替0x00（成功）。
         * 128	0x80	未指明的错误	接收端不接受此消息，且不愿意透露错误原因或没有适用的原因码。
         * 131	0x83	实现特定错误	PUBLISH报文有效，但不被接收端所接受。
         * 135	0x87	未授权	PUBLISH报文未授权。
         * 144	0x90	主题名无效	主题名格式正确，但未被客户端或服务端所接受。
         * 145	0x91	报文标识符被占用	报文标识符已被占用。可能表明客户端和服务端之间的会话状态不匹配。
         * 151	0x97	超出配额	已超出实现限制或管理限制。
         * 153	0x99	载荷格式无效	载荷格式与载荷格式指示符不匹配。
         **/
        TlMqttPubAck res = TlMqttPubAck.build(messageId, PubReasonCode.SUCCESS.getCode(), null, null,mqttVersion);
        channel.writeAndFlush(res);
    }

    /**
     * @description: 发送rec消息给客户端
     * @author hszhou
     * 2025-05-08 16:21:42
     * @param: channel
     * @param: messageId
     * @param: clientId
     * @return: void
     **/
    private void sendRec(Long messageId,Channel channel,MqttVersion mqttVersion) {
        TlMqttPubRecReq res = TlMqttPubRecReq.build(messageId, PubReasonCode.SUCCESS.getCode(), null, null,mqttVersion);
        channel.writeAndFlush(res);
    }


}
