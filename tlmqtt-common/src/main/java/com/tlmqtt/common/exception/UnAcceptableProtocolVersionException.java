package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import io.netty.handler.codec.DecoderException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class UnAcceptableProtocolVersionException extends DecoderException {

    /**
     * 报文错误码
     */
    private final MqttErrorCode errCode;
    /**
     * 报文类型
     */
    private final MqttMessageType mqttMessageType;

    private final Boolean close;

    public UnAcceptableProtocolVersionException(){
        this.errCode = MqttErrorCode.UNACCEPTABLE_PROTOCOL_VERSION;
        this.close = true;
        this.mqttMessageType = MqttMessageType.CONNECT;
    }
}
