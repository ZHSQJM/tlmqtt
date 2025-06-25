package com.tlmqtt.common.model.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnectPayload {

    /**载体*/
    private String clientId;

    /**遗嘱topic*/
    private String willTopic;

    /**遗嘱消息*/
    private String willMessage;

    /**用户名*/
    private String username;

    /**密码*/
    private String  password;





}
