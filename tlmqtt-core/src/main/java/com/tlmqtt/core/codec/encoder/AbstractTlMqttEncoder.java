package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author hszhou
 */
public abstract class AbstractTlMqttEncoder<T extends AbstractTlMessage> extends MessageToByteEncoder<T> {

    @Override
    protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
        encode(ctx, msg, out, msg.getMessageType());
    }


    /**
     * 编码器
     * @author hszhou
     * @datetime: 2025-07-21 10:39:48
     * @param ctx 通道
     * @param msg 消息
     * @param out byteByf
     * @param mqttMessageType 消息类型
     **/
    protected abstract void encode(ChannelHandlerContext ctx, T msg, ByteBuf out, MqttMessageType mqttMessageType);


    /**
     * 计算可变报头长度
     */
    public int calculateVariableByteIntegerLength(int value) {
        if (value == 0) {
            return 1;
        }
        int length = 0;
        do {
            length++;
            value >>>= 7;
        } while (value > 0);
        return length;
    }

    /**
     * 写可变长度
     */
    public void writeVariableByteInteger(ByteBuf out, int length) {
        do {
            int digit = length % 128;
            length = length / 128;
            // 如果还有后续字节，设置最高位为1
            if (length > 0) {
                digit = digit | 0x80;
            }
            out.writeByte(digit);
        } while (length > 0);
    }
}