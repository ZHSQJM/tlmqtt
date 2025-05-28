package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description: 发布确认的响应可变头
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlMqttPubRelVariableHead {


    private Long messageId;

    public static TlMqttPubRelVariableHead build(Long messageId){
        return new TlMqttPubRelVariableHead(messageId);
    }

}
