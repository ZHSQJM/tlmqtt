package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttUnSubscribeVariableHead;
import lombok.*;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttUnSubscribeReq  extends AbstractTlMessage {

    private TlMqttFixedHead fixedHead;

    private TlMqttUnSubscribeVariableHead variableHead;

    private TlMqttUnSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.UNSUBSCRIBE;
    }
}
