package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.payload.TlMqttSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@SuperBuilder
public class TlMqttSubscribeReq  extends AbstractTlMessage  {

    /**可变头*/
    private TlMqttSubscribeVariableHead variableHead;

    private TlMqttSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.SUBSCRIBE;
    }
}
