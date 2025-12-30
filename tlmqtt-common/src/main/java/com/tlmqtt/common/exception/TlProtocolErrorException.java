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
public class TlProtocolErrorException extends TlMqttException {



    public TlProtocolErrorException(MqttMessageType acceptType,MqttMessageType replayTpe) {
        super(MqttErrorCode.PROTOCOL_ERROR, true,acceptType,null,replayTpe);
    }


    public TlProtocolErrorException(MqttErrorCode errorCode, MqttMessageType replayTpe) {
        super(errorCode, true,MqttMessageType.CONNECT,null,replayTpe);
    }

    public TlProtocolErrorException(MqttErrorCode errorCode, MqttMessageType acceptType,MqttMessageType replayTpe) {
        super(errorCode, true,acceptType,null,replayTpe);
    }
}
