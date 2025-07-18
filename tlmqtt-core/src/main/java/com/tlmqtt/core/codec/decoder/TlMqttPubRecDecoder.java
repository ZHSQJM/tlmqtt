package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttPubRecDecoder extends AbstractTlMqttDecoder{


    @Override
    public TlMqttPubRecReq build(ByteBuf buf, int type,int remainingLength) {

        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttPubRecVariableHead variableHead = decodeVariableHeader(buf);
        return new TlMqttPubRecReq(fixedHead, variableHead);

    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PUBREC);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttPubRecVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttPubRecVariableHead.build((long)messageId);
    }

}
