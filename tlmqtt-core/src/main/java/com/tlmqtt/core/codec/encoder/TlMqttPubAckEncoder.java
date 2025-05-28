package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttPubAck;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:35
 * @Description: puback的编码
 */
@ChannelHandler.Sharable
public class TlMqttPubAckEncoder extends MessageToByteEncoder<TlMqttPubAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubAck res, ByteBuf out) throws Exception {

        TlMqttFixedHead fixedHead = res.getFixedHead();
        TlMqttPubAckVariableHead variableHead = res.getVariableHead();
        //回复puback消息
        int messageType = fixedHead.getMessageType().value() << 4;
        //消息类型
        out.writeByte(messageType);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId().intValue());

    }
}
