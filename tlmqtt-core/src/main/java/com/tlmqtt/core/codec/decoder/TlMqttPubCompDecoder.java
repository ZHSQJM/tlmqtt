package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import io.netty.buffer.ByteBuf;

/**
 * @author hszhou
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
