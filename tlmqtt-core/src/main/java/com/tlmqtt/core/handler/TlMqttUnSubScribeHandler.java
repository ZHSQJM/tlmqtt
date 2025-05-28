package com.tlmqtt.core.handler;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;
import com.tlmqtt.core.TlStoreManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: 取消订阅
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttUnSubScribeHandler extends SimpleChannelInboundHandler<TlMqttUnSubscribeReq> {

    private  final TlStoreManager messageService;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttUnSubscribeReq msg) throws Exception {
        String clientId = ctx.channel().attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        TlMqttUnSubscribePayload payload = msg.getPayload();
        List<TlTopic> topics = payload.getTopics();
        //取消订阅 就移除掉主题对应的客户端 这样发送消息就接收不到
        for (TlTopic topic : topics) {
            messageService.getSubscriptionService().unsubscribe(clientId,topic.getName()).subscribe(e->{
                log.debug("client 【{}】 unsubscribe topic 【{}】",clientId,topic);
            });
        }

        //构建ack消息
        int messageId = msg.getVariableHead().getMessageId();
        TlMqttUnSubAck res = TlMqttUnSubAck.of(messageId);
        ctx.channel().writeAndFlush(res);
    }
}
