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
        String clientId = session.getClientId();
        log.debug("【TLMQTT】Handling 【PINGRESP】 event from client:【{}】", clientId);
        Channel channel = ctx.channel();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PINGREQ);
        TlMqttHeartBeatAck res = TlMqttHeartBeatAck.builder().fixedHead(fixedHead).build();
        channel.writeAndFlush(res);
    }
}