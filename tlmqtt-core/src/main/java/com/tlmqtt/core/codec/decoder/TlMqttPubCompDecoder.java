package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author hszhou
 */
public class TlMqttPubCompDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttPubCompReq build(ByteBuf buf,int type, int remainingLength,  TlMqttSession session) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(type,remainingLength);
        TlMqttPubCompVariableHead variableHead = decodeVariableHeader(buf);
        return TlMqttPubCompReq.builder()
            .fixedHead(fixedHead).variableHead(variableHead).build();


    }

    TlMqttFixedHead decodeFixedHeader(int type,int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.PUBCOMP);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }

    TlMqttPubCompVariableHead decodeVariableHeader(ByteBuf buf) {
        int messageId = buf.readUnsignedShort();
        return TlMqttPubCompVariableHead.builder().messageId((long)messageId).build();
    }


}
