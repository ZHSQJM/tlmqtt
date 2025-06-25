package com.tlmqtt.common.model.variable;

/**
 * @author hszhou
 */
public class TlMqttSubAckVariableHead extends TlMqttSubscribeVariableHead {


    public TlMqttSubAckVariableHead(int messageId) {
        super(messageId);
    }

    public static TlMqttSubAckVariableHead of(int messageId){
        return new TlMqttSubAckVariableHead(messageId);
    }
}
