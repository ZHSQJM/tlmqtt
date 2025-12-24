package com.tlmqtt.core.handler;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import com.tlmqtt.common.model.response.TlMqttHeartBeatAck;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlHeartBeatHandler extends AbstractTlHandler<TlMqttHeartBeatReq> {


    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttHeartBeatReq msg, TlMqttSession session) {
        Channel channel = ctx.channel();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PINGRESP);
        TlMqttHeartBeatAck res = TlMqttHeartBeatAck.builder().fixedHead(fixedHead).build();
        channel.writeAndFlush(res);
    }
}