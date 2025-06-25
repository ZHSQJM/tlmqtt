package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import io.netty.buffer.ByteBuf;

/**
 * @author hszhou
 */
public class TlMqttPubRelDecoder extends AbstractTlMqttDecoder{

    @Override
    public TlMqttPubRelReq build(ByteBuf buf, int type,int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttPubRelVariableHead variableHead = decodeVariableHeader(buf);
        return new TlMqttPubRelReq(fixedHead, variableHead);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PUBREL);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttPubRelVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttPubRelVariableHead.build((long)messageId);
    }


}
