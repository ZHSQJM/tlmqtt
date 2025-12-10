package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import lombok.EqualsAndHashCode;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@EqualsAndHashCode(callSuper = true)
public class UnAcceptableProtocolVersionException extends TlMqttException {



    public UnAcceptableProtocolVersionException(){
        super(MqttErrorCode.UNACCEPTABLE_PROTOCOL_VERSION, MqttMessageType.CONNECT);
    }
}
