package com.tlmqtt.common.model.request;

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
@Accessors
@SuperBuilder
@Data
public class TlMqttDisconnectReq extends AbstractTlMessage {

    private TlMqttDisconnectVariableHead variableHead;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.DISCONNECT;
    }
}
