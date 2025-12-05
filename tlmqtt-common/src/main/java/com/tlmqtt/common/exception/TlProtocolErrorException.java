package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import io.netty.handler.codec.DecoderException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 协议错误
 * 在报文解析之后发现包含协议不允许或与客户端或服务端当前状态不一致的数据错误
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TlProtocolErrorException extends DecoderException {

    /**
     * 报文错误码
     */
    private final MqttErrorCode errCode;
    /**
     * 报文类型
     */
    private final MqttMessageType mqttMessageType;

    public TlProtocolErrorException( MqttMessageType mqttMessageType) {
        this.errCode = MqttErrorCode.PROTOCOL_ERROR;
        this.mqttMessageType = mqttMessageType;
    }


    public TlProtocolErrorException(MqttErrorCode errorCode, MqttMessageType mqttMessageType) {
        this.errCode =errorCode;
        this.mqttMessageType = mqttMessageType;
    }
}
