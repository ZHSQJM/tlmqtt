package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:12
 * @Description: 断开连接报文解码器
 */
public class TlMqttDisConnectDecoder  extends AbstractTlMqttDecoder{

    @Override
    public TlMqttDisconnectReq build(ByteBuf buf, int type, int remainingLength){
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        return new TlMqttDisconnectReq(fixedHead);
    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.DISCONNECT);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }
}
