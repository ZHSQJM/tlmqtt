package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import com.tlmqtt.common.model.response.TlMqttHeartBeat;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlHeartBeatHandler extends SimpleChannelInboundHandler<TlMqttHeartBeatReq> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttHeartBeatReq msg) throws Exception {
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【PINGREQ】 event from client:【{}】", clientId);
        TlMqttHeartBeat res = TlMqttHeartBeat.of(MqttMessageType.PINGRESP);
        channel.writeAndFlush(res);
    }
}