package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
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
public class TlMqttException extends DecoderException {

    /**
     * 报文错误码
     */
    private final MqttErrorCode errCode;

    /**
     * 是否关闭连接
     */
    private final Boolean close;

    /**
     * 从哪个报文来的
     */
    private final MqttMessageType acceptType;

    /**
     * 需要发送的报文
     */
    private final MqttMessageType replayType;

    /**
     * Mqtt版本
     */
    private final MqttVersion version;


    private Long messageId;



    public TlMqttException(MqttErrorCode errCode,Boolean close, MqttMessageType acceptType,Long messageId, MqttMessageType replayType){
        this.errCode = errCode;
        this.close = close;
        this.acceptType = acceptType;
        this.replayType = replayType;
        this.version = MqttVersion.MQTT_5;
        this.messageId = messageId;
    }



}
