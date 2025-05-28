package com.tlmqtt.common.model.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hszhou
 * @Date: 2024/11/26 13:29
 * @Description: 发布消息载体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlMqttPublishPayload {

    private Object content;

    public static TlMqttPublishPayload build(String content){
        return new TlMqttPublishPayload(content);
    }

}
