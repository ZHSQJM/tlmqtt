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
public class TlMqttPubCompVariableHead {

    private Long messageId;

    public static TlMqttPubCompVariableHead build(Long messageId) {
        return new TlMqttPubCompVariableHead(messageId);
    }

}
