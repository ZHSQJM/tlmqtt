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
public class TlMqttPubRelVariableHead {


    private Long messageId;

    public static TlMqttPubRelVariableHead build(Long messageId){
        return new TlMqttPubRelVariableHead(messageId);
    }

}
