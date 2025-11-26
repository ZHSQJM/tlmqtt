package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;

/**
 * 无效报文异常
 * 根据规范不能被正确解析的控制报文
 * @author hszhou
 */
public class TlMalformedPacketException extends RuntimeException{


    /**
     * 报文错误码
     */
    private final MqttErrorCode errorCode;
    /**
     * 报文类型
     */
    private final MqttMessageType mqttMessageType;


    public TlMalformedPacketException(MqttErrorCode errorCode,MqttMessageType mqttMessageType){

        this.errorCode = errorCode;
        this.mqttMessageType = mqttMessageType;
    }
}
