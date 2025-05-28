package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttUnSubscribeVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:07
 * @Description: 接收到订阅请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttUnSubscribeReq  extends AbstractTlMessage {

    private TlMqttFixedHead fixedHead;

    private TlMqttUnSubscribeVariableHead variableHead;

    private TlMqttUnSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.UNSUBSCRIBE;
    }
}
