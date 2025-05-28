package com.tlmqtt.common.model.variable;

import com.tlmqtt.common.enums.MqttConnectReturnCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description:
 * mqtt的请求可变头
 * 协议名称 协议 连接标识 保持连接
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnackVariableHead{

    /**会话是否保存*/
    private int currentSession;

    /**连接返回码*/
    private int code;

    public static TlMqttConnackVariableHead build(int currentSession, MqttConnectReturnCode returnCode){
        TlMqttConnackVariableHead variableHead = new TlMqttConnackVariableHead();
        variableHead.setCurrentSession(currentSession);
        variableHead.setCode(returnCode.byteValue());
        return variableHead;
    }

}
