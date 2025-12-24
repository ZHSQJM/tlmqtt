package com.tlmqtt.common.model.response;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttUnSubAckPayload;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.variable.TlMqttUnSubAckVariableHead;
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
public class TlMqttUnSubAck extends AbstractTlMessage {



    private TlMqttUnSubAckVariableHead variableHead;


    private TlMqttUnSubAckPayload payload;
    public static TlMqttUnSubAck build(int messageId) {
        TlMqttUnSubAckVariableHead variableHead = TlMqttUnSubAckVariableHead.builder().messageId(messageId).build();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.UNSUBACK);
        return TlMqttUnSubAck.builder()
            .fixedHead(fixedHead)
            .variableHead(variableHead)
            .build();
    }
    public static TlMqttUnSubAck build(int messageId,int[] reasonCodes) {
        TlMqttUnSubAckVariableHead variableHead = TlMqttUnSubAckVariableHead.builder().messageId(messageId).build();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.UNSUBACK);
        TlMqttUnSubAckPayload payload = TlMqttUnSubAckPayload.builder().codes(reasonCodes).build();
        return TlMqttUnSubAck.builder()
            .fixedHead(fixedHead)
            .variableHead(variableHead)
            .payload(payload)
            .build();
    }



    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.UNSUBACK;
    }
}
