package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlDisconnectHandler extends AbstractTlHandler<TlMqttDisconnectReq> {



    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttDisconnectReq req, TlMqttSession session) {
        Channel channel = ctx.channel();
        String clientId = session.getClientId();
        log.info("Handling 【DISCONNECT】 event from client:【{}】", clientId);
        //断开标志位设置为true 这样就不发生遗嘱消息了
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(true);
        channel.close();
    }


}