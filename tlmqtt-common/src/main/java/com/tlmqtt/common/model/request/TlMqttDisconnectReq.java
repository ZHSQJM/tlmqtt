package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttDisconnectVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
@Data
public class TlMqttDisconnectReq extends AbstractTlMessage {

    private TlMqttDisconnectVariableHead variableHead;


    public static TlMqttDisconnectReq build(MqttErrorCode errorCode){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.builder().messageType(MqttMessageType.DISCONNECT).build();
        TlMqttDisconnectVariableHead vh = TlMqttDisconnectVariableHead.builder()
            .reasonCode(errorCode.byteValue()).build();
      return TlMqttDisconnectReq.builder().fixedHead(fixedHead)
            .variableHead(vh).build();
    }
    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.DISCONNECT;
    }
}
