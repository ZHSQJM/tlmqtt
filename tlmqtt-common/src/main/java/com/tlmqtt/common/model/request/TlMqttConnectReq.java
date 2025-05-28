package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @Author: hszhou
 * @Date: 2024/11/25 11:07
 * @Description: 接收到mqtt的连接报文
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnectReq  extends AbstractTlMessage {

    private TlMqttFixedHead fixedHead;
    /**可变头*/
    private TlMqttConnectVariableHead variableHead;

    private TlMqttConnectPayload payload;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.CONNECT;
    }
}
