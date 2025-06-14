package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubAckPayload;
import com.tlmqtt.common.model.variable.TlMqttSubAckVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:12
 * @Description: 订阅响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttSubAck {


    private TlMqttFixedHead fixedHead;

    private TlMqttSubAckVariableHead variableHead;

    private TlMqttSubAckPayload payload;

    public static TlMqttSubAck of(int[] codes,int messageId) {
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.SUBACK);
        TlMqttSubAckVariableHead variableHead = TlMqttSubAckVariableHead.of(messageId);
        TlMqttSubAckPayload payload = TlMqttSubAckPayload.build(codes);
        return new TlMqttSubAck(fixedHead, variableHead, payload);
    }
}
