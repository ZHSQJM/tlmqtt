package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * @author hszhou
 */
@ChannelHandler.Sharable
public class TlMqttPubRecEncoder extends MessageToByteEncoder<TlMqttPubRecReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubRecReq res, ByteBuf out) throws Exception {
        TlMqttPubRecVariableHead variableHead = res.getVariableHead();

        //回复订阅
        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId().intValue());
    }
}
