package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import lombok.*;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttPubCompReq extends AbstractTlMessage{

    private TlMqttFixedHead fixedHead;

    private TlMqttPubCompVariableHead variableHead;

    public static TlMqttPubCompReq build(Long messageId){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBCOMP);
        TlMqttPubCompVariableHead variableHead  = TlMqttPubCompVariableHead.build(messageId);
        return new TlMqttPubCompReq(fixedHead,variableHead);
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBCOMP;
    }
}
