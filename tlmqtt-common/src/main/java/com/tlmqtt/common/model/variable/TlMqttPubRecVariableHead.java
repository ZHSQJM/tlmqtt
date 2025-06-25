package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
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
