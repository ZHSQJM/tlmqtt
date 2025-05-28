package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import lombok.*;

/**
 * @Author: hszhou
 * @Date: 2024/11/26 15:07
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class TlMqttPubRelReq extends AbstractTlMessage {

    private TlMqttFixedHead fixedHead;

    private TlMqttPubRelVariableHead variableHead;

    public static TlMqttPubRelReq build(Long messageId){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBREL);
        TlMqttPubRelVariableHead variableHead= TlMqttPubRelVariableHead.build(messageId);
        return new TlMqttPubRelReq(fixedHead,variableHead);
    }

    public static TlMqttPubRelReq build(Long messageId,boolean isDup){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBREL, MqttQoS.AT_MOST_ONCE,isDup,false);
        TlMqttPubRelVariableHead variableHead= TlMqttPubRelVariableHead.build(messageId);
        return new TlMqttPubRelReq(fixedHead,variableHead);
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBREL;
    }
}
