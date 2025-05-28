package com.tlmqtt.core.handler;

import com.tlmqtt.bridge.TlBridgeManager;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttPubAck;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.TlStoreManager;
import com.tlmqtt.core.channel.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: publish处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttPublishHandler extends SimpleChannelInboundHandler<TlMqttPublishReq> {

    private final TlStoreManager messageService;


    private final ChannelManager channelManager;


    private final TlBridgeManager bridgeManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttPublishReq req) throws Exception {

        log.debug("in【PUBLISH】 handler");
        Channel channel = ctx.channel();
        Object clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get();
        if (clientId == null) {
            channel.close();
            return;
        }



        TlMqttFixedHead fixedHead = req.getFixedHead();
        TlMqttPublishVariableHead variableHead = req.getVariableHead();
        TlMqttPublishPayload payload = req.getPayload();

        boolean retain = fixedHead.isRetain();
        MqttQoS messageQos = fixedHead.getQos();
        String topic = variableHead.getTopic();
        Long messageId = variableHead.getMessageId();
        String content = payload.getContent().toString();
        if(content.equals("bye")){
            channel.close();
        }
        /*如果是保留消息 存储*/
        if (retain) {
            storeRetain(topic, content, messageQos, messageId, clientId.toString()).subscribe();
        }
        //数据转发到其他服务
        PublishMessage publishMessage = PublishMessage.build(messageId, topic, clientId.toString(), content, messageQos.value(),
            retain, false);
        bridgeManager.send(publishMessage);
        log.info("broker receive message qos 【{}】,messageId 【{}】",messageQos.value(),messageId);
        switch (messageQos) {
            case AT_LEAST_ONCE -> sendAck(messageId, clientId.toString());
            case EXACTLY_ONCE -> {
                //这里需要保存消息 key是messageId，value是req，在收到rel消息后 需要将这个消息转发到其他订阅的客户端 在rel只能收到messageId，没有其他的信息
                channel.attr(AttributeKey.valueOf(Constant.PUB_MSG)).set(req);
                //发送rec消息给发送者
                sendRec(messageId, clientId.toString());
                return;
            }
            default -> log.error("error qos level");
        }
        //转发给其他的订阅的客户端
        messageService.publish(topic,  messageQos, content);
    }

    /**
     * @description: 在新订阅的时候发送
     * @author: hszhou
     * @datetime: 2025-04-29 18:29:08
     * @param:
     * @param: topic 主题
     * @param: content 内容
     * @param: qos
     * @param: messageId
     * @return: Mono<Boolean>
     **/
    private Mono<Boolean> storeRetain(String topic, String content, MqttQoS qos, Long messageId,String clientId) {

        return Mono.defer(()->{
            if ("".equals(content) || null==content) {
                return messageService.getRetainService().clear(topic);
            }else{
                PublishMessage message = new PublishMessage();
                message.setRetain(true);
                message.setMessageId(messageId);
                message.setQos(qos.value());
                message.setTopic(topic);
                message.setMessage(content);
                message.setDup(false);
                message.setClientId(clientId);
                return messageService.getRetainService().save(topic, message);
            }
        });
    }


    /**
     * 构建ack消息发送返回
     *
     * @param messageId 消息ID
     * @param clientId  客户端ID
     */
    private void sendAck(Long messageId, String clientId) {
        log.info("broker send  messageId 【{}】 ack to client 【{}】",messageId,clientId);
        TlMqttPubAck res = TlMqttPubAck.build(messageId);
        channelManager.writeAndFlush(clientId,res);
    }

    /**
     * @description: 发送rec消息给客户端
     * @author: hszhou
     * @datetime: 2025-05-08 16:21:42
     * @param:
     * @param: messageId
     * @param: clientId
     * @return: void
     **/
    private void sendRec( Long messageId, String clientId) {
        log.debug("broker send rec messageId 【{}】 to clientId 【{}】",messageId,clientId);
        TlMqttPubRecReq res = TlMqttPubRecReq.build(messageId);
        channelManager.writeAndFlush(clientId,res);
    }
}
