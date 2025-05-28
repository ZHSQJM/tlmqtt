package com.tlmqtt.core.handler;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import com.tlmqtt.common.model.response.TlMqttHeartBeat;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: 心跳处理器
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttHeartBeatHandler extends SimpleChannelInboundHandler<TlMqttHeartBeatReq> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttHeartBeatReq msg) throws Exception {
        log.debug("in 【PINGREQ】  handler");
        TlMqttHeartBeat res =TlMqttHeartBeat.of(MqttMessageType.PINGRESP);
        ctx.channel().writeAndFlush(res);
    }
}
