package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubAckPayload;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.variable.TlMqttSubAckVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Accessors
@SuperBuilder
public class TlMqttSubAck  extends AbstractTlMessage {



    private TlMqttSubAckVariableHead variableHead;

    private TlMqttSubAckPayload payload;

    public static TlMqttSubAck build(int[] codes,int messageId,String reasonString,List<UserProperty> userPropertyList) {
        TlMqttSubAckVariableHead variableHead = TlMqttSubAckVariableHead.builder().messageId(messageId)
            .reasonString(reasonString)
            .userPropertyList(userPropertyList)
            .build();
        TlMqttSubAckPayload payload = TlMqttSubAckPayload.builder().codes(codes).build();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.SUBACK);
        return TlMqttSubAck.builder()
            .fixedHead(fixedHead).variableHead(variableHead).payload(payload).build();
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.SUBACK;
    }
}
