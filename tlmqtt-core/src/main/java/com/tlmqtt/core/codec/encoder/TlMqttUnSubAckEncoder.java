package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;
import com.tlmqtt.common.model.variable.TlMqttUnSubAckVariableHead;
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
public class TlMqttUnSubAckEncoder extends MessageToByteEncoder<TlMqttUnSubAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttUnSubAck req, ByteBuf out) throws Exception {
        TlMqttUnSubAckVariableHead variableHead = req.getVariableHead();
        TlMqttFixedHead fixedHead = req.getFixedHead();
        int type = fixedHead.getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId());

    }
}