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
public class TlMqttUnSubAckVariableHead {

    private int messageId;

    public static TlMqttUnSubAckVariableHead build(int messageId){
        return new TlMqttUnSubAckVariableHead(messageId);
    }

}
