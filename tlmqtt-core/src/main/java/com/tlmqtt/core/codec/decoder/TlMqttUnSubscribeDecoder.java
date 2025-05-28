package com.tlmqtt.core.codec.decoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.variable.TlMqttUnSubscribeVariableHead;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: 订阅确定解码器
 */
public class TlMqttUnSubscribeDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttUnSubscribeReq build(ByteBuf buf, int type,int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttUnSubscribeVariableHead variableHead = decodeVariableHeader(buf);
        TlMqttUnSubscribePayload payload = decodePayLoad(buf);
        return new TlMqttUnSubscribeReq(fixedHead, variableHead, payload);
    }


    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.UNSUBSCRIBE);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttUnSubscribeVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        TlMqttUnSubscribeVariableHead variableHead = new TlMqttUnSubscribeVariableHead();
        variableHead.setMessageId(messageId);
        return variableHead;
    }


    TlMqttUnSubscribePayload decodePayLoad(ByteBuf buf) {
        TlMqttUnSubscribePayload payload = new TlMqttUnSubscribePayload();
        List<TlTopic> topics = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while (buf.readableBytes() != 0) {
            TlTopic topic = new TlTopic();
            int topicFilterLength = buf.readUnsignedShort();
            byte[] topicFilter = new byte[topicFilterLength];
            buf.readBytes(topicFilter);
            String topicFilterStr = new String(topicFilter);
            topic.setName(topicFilterStr);
            topics.add(topic);
            sb.append(topicFilterStr).append(" ");
        }
        payload.setTopics(topics);
        return payload;
    }
}
