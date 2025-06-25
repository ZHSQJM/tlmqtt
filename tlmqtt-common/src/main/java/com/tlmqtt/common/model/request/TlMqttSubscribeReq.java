package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
import lombok.*;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttSubscribeReq  extends AbstractTlMessage  {

    private TlMqttFixedHead fixedHead;

    /**可变头*/
    private TlMqttSubscribeVariableHead variableHead;

    private TlMqttSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.SUBSCRIBE;
    }
}
