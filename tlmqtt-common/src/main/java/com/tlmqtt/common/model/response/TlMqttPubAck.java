package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttPubAck  {

    private TlMqttFixedHead fixedHead;

    private TlMqttPubAckVariableHead variableHead;

    public static TlMqttPubAck build(Long messageId){
        TlMqttFixedHead fixedHead =TlMqttFixedHead.build(MqttMessageType.PUBACK);
        TlMqttPubAckVariableHead tlMqttPubAckVariableHead = TlMqttPubAckVariableHead.build(messageId);
        return new TlMqttPubAck(fixedHead,tlMqttPubAckVariableHead);
    }


}
