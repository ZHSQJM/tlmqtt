package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/26 15:07
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
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
