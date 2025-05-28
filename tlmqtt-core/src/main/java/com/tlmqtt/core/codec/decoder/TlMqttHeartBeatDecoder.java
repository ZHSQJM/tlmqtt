package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: 心跳包解码器
 */
public class TlMqttHeartBeatDecoder  extends AbstractTlMqttDecoder{

    @Override
    public TlMqttHeartBeatReq build(ByteBuf buf, int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        return new TlMqttHeartBeatReq(fixedHead);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PINGRESP);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }

}
