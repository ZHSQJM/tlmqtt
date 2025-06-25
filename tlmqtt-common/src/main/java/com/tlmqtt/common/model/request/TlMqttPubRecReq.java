package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
import lombok.*;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttPubRecReq extends AbstractTlMessage {

    private TlMqttFixedHead fixedHead;

    private TlMqttPubRecVariableHead variableHead;

    public static TlMqttPubRecReq build(Long messageId) {
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBREC);
        TlMqttPubRecVariableHead variableHead = TlMqttPubRecVariableHead.build(messageId);
        return new TlMqttPubRecReq(fixedHead, variableHead);
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBREC;
    }
}
