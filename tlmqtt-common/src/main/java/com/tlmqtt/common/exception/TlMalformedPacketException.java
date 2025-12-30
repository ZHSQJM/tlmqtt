package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import io.netty.handler.codec.DecoderException;

/**
 * 无效报文异常
 * 根据规范不能被正确解析的控制报文
 * @author hszhou
 */
public class TlMalformedPacketException extends TlMqttException {


    public TlMalformedPacketException(MqttMessageType acceptType,MqttMessageType replayType) {
        super(MqttErrorCode.MALFORMED_MESSAGE, true, acceptType,null,replayType);
    }



}