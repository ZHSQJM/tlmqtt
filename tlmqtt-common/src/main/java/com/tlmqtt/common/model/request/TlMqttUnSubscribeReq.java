package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttUnSubscribeVariableHead;
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
public class TlMqttUnSubscribeReq  extends AbstractTlMessage {

    private TlMqttUnSubscribeVariableHead variableHead;

    private TlMqttUnSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.UNSUBSCRIBE;
    }
}
