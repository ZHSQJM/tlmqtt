package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttConnectReturnCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.variable.TlMqttConnackVariableHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:12
 * @Description: 连接请求的响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnack  {

    private TlMqttFixedHead fixedHead;
    private TlMqttConnackVariableHead variableHead;
    public static TlMqttConnack build(int currentSession, MqttConnectReturnCode returnCode){
        TlMqttConnack res = new TlMqttConnack();
        TlMqttFixedHead fixedHead= TlMqttFixedHead.build(MqttMessageType.CONNACK);
        res.setFixedHead(fixedHead);
        TlMqttConnackVariableHead variableHead =TlMqttConnackVariableHead.build(currentSession,returnCode);
        res.setVariableHead(variableHead);
        return res;
    }
}
