package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:35
 * @Description: puback的编码
 */
@ChannelHandler.Sharable
public class TlMqttPublishEncoder extends MessageToByteEncoder<TlMqttPublishReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPublishReq req, ByteBuf out) throws Exception {

        TlMqttFixedHead fixedHead = req.getFixedHead();
        TlMqttPublishVariableHead variableHead = req.getVariableHead();

        int type = getFixedHeaderByte(fixedHead);
        MqttQoS qos = fixedHead.getQos();
        //消息类型
        out.writeByte(type);

        String topic = variableHead.getTopic();
        byte[] topicBytes = topic.getBytes();
        Object content = req.getPayload().getContent();
        byte[] bytes = content.toString().getBytes();
        // 2个字节的主题名称+主题字节的长度+载荷的长度 //如果消息的qos不是0  加上2个自己的消息长度
        int remindLength = Short.BYTES + topic.getBytes().length + bytes.length;
        if (qos != MqttQoS.AT_MOST_ONCE) {
            remindLength = remindLength + Short.BYTES;
        }
        //剩余长度
        writeRemainingLength(out, remindLength);
        out.writeShort(topicBytes.length);
        out.writeBytes(topicBytes);
        Long messageId = variableHead.getMessageId();
        if (qos != MqttQoS.AT_MOST_ONCE) {
            out.writeShort(messageId.intValue());
        }
        out.writeBytes(bytes);
    }


    private void writeRemainingLength(ByteBuf out, int remainingLength) {
        do {
            int digit = remainingLength % 128;
            remainingLength = remainingLength / 128;
            // 如果还有后续字节，设置最高位为1
            if (remainingLength > 0) {
                digit = digit | 0x80;
            }
            out.writeByte(digit);
        } while (remainingLength > 0);
    }

    private int getFixedHeaderByte(TlMqttFixedHead header) {
        int ret = 0;
        ret |= header.getMessageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.getQos().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return ret;
    }

}
