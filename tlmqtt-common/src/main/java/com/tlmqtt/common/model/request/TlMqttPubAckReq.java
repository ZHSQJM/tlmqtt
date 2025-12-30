package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@SuperBuilder
public class TlMqttPubAckReq extends AbstractTlMessage {


    private TlMqttPubAckVariableHead variableHead;



    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBACK;
    }
}
