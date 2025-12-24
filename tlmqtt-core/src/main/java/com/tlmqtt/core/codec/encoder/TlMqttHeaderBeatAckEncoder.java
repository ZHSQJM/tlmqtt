package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttHeartBeatAck;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
public class TlMqttHeaderBeatAckEncoder extends AbstractTlMqttEncoder<TlMqttHeartBeatAck> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttHeartBeatAck res, ByteBuf out, MqttMessageType mqttMessageType){
        TlMqttFixedHead fixedHead = res.getFixedHead();
        int messageType = fixedHead.getMessageType().value() << 4;
        out.writeByte(messageType);
        out.writeByte(0);

    }

}
