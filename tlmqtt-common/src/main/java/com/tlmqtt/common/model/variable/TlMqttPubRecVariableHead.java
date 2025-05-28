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
public class TlMqttPubRecVariableHead {

    private Long messageId;

    public static TlMqttPubRecVariableHead build(Long messageId){
        return new TlMqttPubRecVariableHead(messageId);
    }
}
