package com.tlmqtt.common.model;

import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author: hszhou
 * @Date: 2024/11/28 15:40
 * @Description: mqtt的会话
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlMqttSession {
    /**客户端标识*/
    private String clientId;
    /**断开时 是否清除会话*/
    private Boolean cleanSession;
    /**订阅的主题*/
    private Set<String> topics =new HashSet<>();
    /**协议版本*/
    private MqttVersion mqttVersion;

    public static TlMqttSession build(String clientId, Boolean cleanSession){
        TlMqttSession session=new TlMqttSession();
        session.setClientId(clientId);
        session.setCleanSession(cleanSession);
        session.setMqttVersion(MqttVersion.MQTT_3_1_1);
        return session;
    }


}
