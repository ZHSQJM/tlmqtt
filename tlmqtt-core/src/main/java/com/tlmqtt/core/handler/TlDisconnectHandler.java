package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.variable.TlMqttDisconnectVariableHead;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlDisconnectHandler extends AbstractTlHandler<TlMqttDisconnectReq> {

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttDisconnectReq req, TlMqttSession session) {
        Channel channel = ctx.channel();
        String clientId = session.getClientId();
        log.debug("Handling 【DISCONNECT】 event from client:【{}】", clientId);
        //断开标志位设置为true 这样就不发生遗嘱消息了
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(true);
        TlMqttDisconnectVariableHead variableHead = req.getVariableHead();
        int reasonCode = variableHead.getReasonCode();
     //   log.info("Disconnect reasonCode:【{}】", reasonCode);
      //  log.error("关闭连接");
        channel.close();
    }


}