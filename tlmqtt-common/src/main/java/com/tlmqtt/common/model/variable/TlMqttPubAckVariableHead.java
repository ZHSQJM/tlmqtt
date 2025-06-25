package com.tlmqtt.common.model.variable;

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
public class TlMqttPubAckVariableHead {



    private Long messageId;


    public static TlMqttPubAckVariableHead build(Long messageId){
        return new TlMqttPubAckVariableHead(messageId);
    }

}
