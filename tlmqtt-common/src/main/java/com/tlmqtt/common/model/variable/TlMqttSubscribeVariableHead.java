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
public class TlMqttSubscribeVariableHead {

    private int messageId;

    public static TlMqttSubscribeVariableHead build(int messageId){
      return new TlMqttSubscribeVariableHead(messageId);
    }
}
