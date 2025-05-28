package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.TlStoreManager;
import com.tlmqtt.core.channel.ChannelManager;
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
 * @Description: 主动断开处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttDisconnectHandler extends SimpleChannelInboundHandler<TlMqttDisconnectReq> {


    private final TlStoreManager messageService;


    private final ChannelManager channelManager;



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttDisconnectReq msg) throws Exception {

        log.debug("in 【DISCONNECT】 handler");
        //断开标志位设置为true 这样就不发生遗嘱消息了
        ctx.channel().attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(true);
        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)  {

        //判断是否是正常断开的
        Boolean disconnectFlag= (Boolean)ctx.channel().attr(AttributeKey.valueOf(Constant.DISCONNECT)).get();
        //判定是否是mqtt连接
        Object clientIdObj = ctx.channel().attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get();
        if(null==clientIdObj){
            ctx.channel().close();
            return;
        }
        String clientId = clientIdObj.toString();
        log.info("client 【{}】 disconnect",clientId);
        messageService.getSessionService().find(clientId)
                .flatMap(session -> {
                    channelManager.remove(clientId);
                    /*当CleanSession为0的会话断开后，服务器还必须将所有和客户端订阅相关的QoS1和QoS2的消息作为会话状态的一部分存储起来[MQTT-3.1.2-5]。*/
                    return session.getCleanSession()?messageService.clearAll(clientId): Mono.empty();
                }).subscribe();

        if(disconnectFlag){
            //如果断开标志位是true 表示是正常断开的 就不发生遗嘱消息
            messageService.getPublishService().clearWill(clientId).subscribe();
            return;
        }
        messageService.getPublishService().findWill(clientId)
            .flatMap(willMsg -> {
                String topic = willMsg.getTopic();
                MqttQoS qos = MqttQoS.valueOf(willMsg.getQos());
                TlMqttPublishReq req = TlMqttPublishReq.build(topic,qos,false,willMsg.getMessage());
                if(qos!=MqttQoS.AT_MOST_ONCE){
                    Long messageId = messageService.getMessageId();
                    req.getVariableHead().setMessageId(messageId);
                }
                return messageService.getSubscriptionService().find(topic)
                        // 向所有订阅者发送消息
                        .doOnNext(sub ->
                                channelManager.writeAndFlush(sub.getClientId(), req)
                        )
                        // 发送完成后清理遗嘱消息
                        .then(Mono.defer(() ->
                            messageService.getPublishService().clearWill(clientId)
                        ));
            })
            .subscribe();
    }


}
