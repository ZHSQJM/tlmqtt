package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.model.payload.TlMqttSubAckPayload;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.common.model.variable.TlMqttSubAckVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
/**
 * @author hszhou
 */
@ChannelHandler.Sharable
public class TlMqttSubAckEncoder extends MessageToByteEncoder<TlMqttSubAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttSubAck res, ByteBuf out) throws Exception {
        TlMqttSubAckVariableHead variableHead = res.getVariableHead();
        TlMqttSubAckPayload payload = res.getPayload();
        int[] codes = payload.getCodes();
        //回复订阅
        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES + codes.length);
        out.writeShort(variableHead.getMessageId());
        for (int code : codes) {
            out.writeByte(code);
        }
    }
}