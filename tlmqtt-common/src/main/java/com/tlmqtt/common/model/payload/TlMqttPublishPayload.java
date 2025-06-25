package com.tlmqtt.common.model.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
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
