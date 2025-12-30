package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import io.netty.handler.codec.DecoderException;

/**
 * 认证失败异常
 *
 * @author hszhou
 */
public class TlAuthenticationException extends TlMqttException {


    public TlAuthenticationException(boolean close,MqttMessageType acceptType,MqttMessageType replayType){
        super(MqttErrorCode.UNAUTHORIZED,close,acceptType,null,replayType);
    }
}
