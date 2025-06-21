package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2025/6/5 18:57
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlDisconnectHandler extends SimpleChannelInboundHandler<TlMqttDisconnectReq> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttDisconnectReq msg) throws Exception {
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【DISCONNECT】 event from client:【{}】", clientId);
        //断开标志位设置为true 这样就不发生遗嘱消息了
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(true);
        channel.close();
    }
}