package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubscribePayload;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
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
public class TlMqttSubscribeReq  extends AbstractTlMessage  {

    private TlMqttFixedHead fixedHead;

    /**可变头*/
    private TlMqttSubscribeVariableHead variableHead;

    private TlMqttSubscribePayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.SUBSCRIBE;
    }
}
