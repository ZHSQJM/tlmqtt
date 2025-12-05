package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.response.TlMqttHeartBeatAck;
import com.tlmqtt.common.model.variable.TlMqttDisconnectVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@ChannelHandler.Sharable
public class TlMqttDisconnectEncoder extends AbstractTlMqttEncoder<TlMqttDisconnectReq> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttDisconnectReq res, ByteBuf out, MqttMessageType
    mqttMessageType){
        TlMqttFixedHead fixedHead = res.getFixedHead();

        TlMqttDisconnectVariableHead variableHead = res.getVariableHead();

        out.writeByte(fixedHead.getMessageType().value() << 4);
        writeVariableByteInteger(out,1);
        out.writeByte(variableHead.getReasonCode());
    }

}
