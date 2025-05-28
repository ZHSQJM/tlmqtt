package com.tlmqtt.core.codec.decoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: 订阅消息解码器
 */
public class TlMqttSubscribeDecoder extends AbstractTlMqttDecoder {


    @Override
    public TlMqttSubscribeReq build(ByteBuf buf, int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttSubscribeVariableHead variableHead = decodeVariableHeader(buf);
        TlMqttSubscribePayload payload = decodePayLoad(buf);
        return new TlMqttSubscribeReq(fixedHead, variableHead, payload);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.SUBSCRIBE);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttSubscribeVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttSubscribeVariableHead.build(messageId);
    }


    TlMqttSubscribePayload decodePayLoad(ByteBuf buf) {

        int i = buf.readableBytes();
        if (i == 0) {
            return null;
        }
        TlMqttSubscribePayload payload = new TlMqttSubscribePayload();
        List<TlTopic> topics = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while (buf.readableBytes() != 0) {
            TlTopic topic = new TlTopic();
            int topicFilterLength = buf.readUnsignedShort();
            byte[] topicFilter = new byte[topicFilterLength];
            buf.readBytes(topicFilter);
            String topicFilterStr = new String(topicFilter);
            short qos = buf.readUnsignedByte();
            topic.setName(topicFilterStr);
            topic.setQos(qos);
            topics.add(topic);
            sb.append(topicFilterStr).append(" ");

        }
        payload.setTopics(topics);
        return payload;
    }
}
