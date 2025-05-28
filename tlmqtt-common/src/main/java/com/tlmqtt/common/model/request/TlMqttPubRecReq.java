package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
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
