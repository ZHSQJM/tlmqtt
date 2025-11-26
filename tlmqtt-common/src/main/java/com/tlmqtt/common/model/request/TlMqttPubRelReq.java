package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Accessors
@SuperBuilder
public class TlMqttPubRelReq extends AbstractTlMessage {


    private TlMqttPubRelVariableHead variableHead;

    public static TlMqttPubRelReq build(Long messageId){

        TlMqttPubRelVariableHead variableHead= TlMqttPubRelVariableHead.builder().messageId(messageId).build();
        TlMqttFixedHead fixedHead= TlMqttFixedHead.build(MqttMessageType.PUBREL, MqttQoS.AT_LEAST_ONCE, false);
        return TlMqttPubRelReq.builder()
            .fixedHead(fixedHead)
            .variableHead(variableHead).build();
    }



    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBREL;
    }
}
