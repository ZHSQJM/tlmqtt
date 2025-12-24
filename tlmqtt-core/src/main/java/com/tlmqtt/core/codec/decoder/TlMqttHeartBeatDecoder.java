package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttHeartBeatReq;
import io.netty.buffer.ByteBuf;


/**
 * @author hszhou
 */
public class TlMqttHeartBeatDecoder  extends AbstractTlMqttDecoder{


    public TlMqttHeartBeatDecoder(MqttConfiguration configuration){
        super(configuration);
    }
    @Override
    public TlMqttHeartBeatReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        return TlMqttHeartBeatReq.builder().fixedHead(fixedHead).build();

    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        return TlMqttFixedHead.builder()
            .messageType(MqttMessageType.PINGREQ)
            .length(remainingLength).build();
    }

}
