package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@SuperBuilder
public class TlMqttConnectReq  extends AbstractTlMessage {

    /**可变头*/
    private TlMqttConnectVariableHead variableHead;

    private TlMqttConnectPayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.CONNECT;
    }
}
