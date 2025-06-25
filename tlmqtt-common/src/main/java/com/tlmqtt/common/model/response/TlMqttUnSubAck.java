package com.tlmqtt.common.model.response;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttUnSubAckVariableHead;
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
public class TlMqttUnSubAck  {

    private TlMqttFixedHead fixedHead;

    private TlMqttUnSubAckVariableHead variableHead;


    public static TlMqttUnSubAck of(int messageId) {
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.UNSUBACK);
        TlMqttUnSubAckVariableHead variableHead = TlMqttUnSubAckVariableHead.build(messageId);
        return new TlMqttUnSubAck(fixedHead,variableHead);
    }

}
