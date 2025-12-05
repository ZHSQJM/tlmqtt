package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import io.netty.handler.codec.DecoderException;

/**
 * 无效报文异常
 * 根据规范不能被正确解析的控制报文
 * @author hszhou
 */
public class TlMalformedPacketException extends DecoderException {


    /**
     * 报文错误码
     */
    private final MqttErrorCode errorCode;
    /**
     * 报文类型
     */
    private final MqttMessageType mqttMessageType;


    public TlMalformedPacketException(MqttMessageType mqttMessageType){
        this.errorCode = MqttErrorCode.MALFORMED_MESSAGE;
        this.mqttMessageType = mqttMessageType;
    }
    
    public MqttErrorCode getErrorCode() {
        return errorCode;
    }
    
    public MqttMessageType getMqttMessageType() {
        return mqttMessageType;
    }
}