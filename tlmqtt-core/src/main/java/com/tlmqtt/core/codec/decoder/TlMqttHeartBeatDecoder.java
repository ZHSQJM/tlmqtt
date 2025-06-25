package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import io.netty.buffer.ByteBuf;

/**
 * @author hszhou
 */
public class TlMqttHeartBeatDecoder  extends AbstractTlMqttDecoder{

    @Override
    public TlMqttHeartBeatReq build(ByteBuf buf, int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        return new TlMqttHeartBeatReq(fixedHead);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PINGREQ);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }

}
