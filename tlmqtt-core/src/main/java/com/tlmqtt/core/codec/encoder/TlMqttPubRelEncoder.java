package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
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
public class TlMqttPubRelEncoder extends MessageToByteEncoder<TlMqttPubRelReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubRelReq res, ByteBuf out) throws Exception {
        TlMqttPubRelVariableHead variableHead = res.getVariableHead();

        //回复订阅
        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId().intValue());
    }
}
