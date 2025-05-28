package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@ChannelHandler.Sharable
public class TlMqttPubCompEncoder extends MessageToByteEncoder<TlMqttPubCompReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubCompReq res, ByteBuf out) throws Exception {
        TlMqttPubCompVariableHead variableHead = res.getVariableHead();

        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId().intValue());
    }
}
