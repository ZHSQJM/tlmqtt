package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: QOS2的消息接受者发给发送者的报文确定解码器
 */
public class TlMqttPubCompDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttPubCompReq build(ByteBuf buf,int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttPubCompVariableHead variableHead = decodeVariableHeader(buf);
        return new TlMqttPubCompReq(fixedHead, variableHead);

    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PUBCOMP);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }

    TlMqttPubCompVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttPubCompVariableHead.build((long)messageId);
    }


}
