package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description: 发布消息的可变头
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TlMqttPublishVariableHead {


    private String topic;
    private Long messageId;

    public TlMqttPublishVariableHead(String topic) {
        this.topic = topic;
    }

    public static TlMqttPublishVariableHead build(String topic, Long messageId){
        return new TlMqttPublishVariableHead(topic,messageId);
    }

    public static TlMqttPublishVariableHead build(String topic){
        return new TlMqttPublishVariableHead(topic);
    }


}
