package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;

/**
 * 协议错误
 * 在报文解析之后发现包含协议不允许或与客户端或服务端当前状态不一致的数据错误
 * @author hszhou
 */
public class TlProtocolErrorException extends RuntimeException{


    private final MqttErrorCode errcode;

    private final MqttMessageType mqttMessageType;



    public TlProtocolErrorException(MqttErrorCode errcode, MqttMessageType mqttMessageType) {
        this.errcode = errcode;
        this.mqttMessageType = mqttMessageType;
    }



}
