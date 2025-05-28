package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description: 发布确认的响应可变头
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttPubAckVariableHead {



    private Long messageId;


    public static TlMqttPubAckVariableHead build(Long messageId){
        return new TlMqttPubAckVariableHead(messageId);
    }

}
