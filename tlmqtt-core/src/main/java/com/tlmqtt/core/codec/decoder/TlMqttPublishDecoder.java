package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import io.netty.buffer.ByteBuf;


/**
 * @author hszhou
 */
public class TlMqttPublishDecoder extends AbstractTlMqttDecoder{

    @Override
    public TlMqttPublishReq build(ByteBuf buf, int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(type, remainingLength);
        MqttQoS qos = fixedHead.getQos();
        TlMqttPublishVariableHead variableHead = decodeVariableHeader(buf, qos);
        TlMqttPublishPayload payload = decodePayLoad(buf);
        return new TlMqttPublishReq(fixedHead, variableHead, payload);
    }

    TlMqttFixedHead decodeFixedHeader(int type, int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        int messageType = type >> 4;
        int retain = (type) & 1;
        int qos = (type >> 1) & 3;
        int dup = (type >> 3) & 1;
        fixedHead.setMessageType(MqttMessageType.valueOf(messageType));
        fixedHead.setDup(dup != 0);
        fixedHead.setQos(MqttQoS.valueOf(qos));
        fixedHead.setRetain(retain != 0);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttPublishVariableHead decodeVariableHeader(ByteBuf buf, MqttQoS qos) {
        int topicLength = buf.readUnsignedShort();
        byte[] topic = new byte[topicLength];
        buf.readBytes(topic);
        String topicName = new String(topic);
        if (qos != MqttQoS.AT_MOST_ONCE) {
            int messageId = buf.readUnsignedShort();
            return TlMqttPublishVariableHead.build(topicName, (long) messageId);
        }
        return TlMqttPublishVariableHead.build(topicName);
    }


    TlMqttPublishPayload decodePayLoad(ByteBuf buf) {
        int contentLength = buf.readableBytes();
        byte[] contentByte = new byte[contentLength];
        buf.readBytes(contentByte);
        String content = new String(contentByte);
        return TlMqttPublishPayload.build(content);
    }
}
