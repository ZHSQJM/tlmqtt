package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import io.netty.buffer.ByteBuf;

/**
 * @author hszhou
 */
public class TlMqttPubAckDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttPubAckReq build(ByteBuf buf, int type,int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttPubAckVariableHead variableHead = decodeVariableHeader(buf);
        return new TlMqttPubAckReq(fixedHead, variableHead);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setLength(remainingLength);
        fixedHead.setMessageType(MqttMessageType.PUBACK);
        return fixedHead;
    }

    TlMqttPubAckVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttPubAckVariableHead.build((long)messageId);
    }

}
