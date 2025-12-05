package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttErrorCode;
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

public class TopicAliasInvalidException extends DecoderException {

    /**
     * 报文错误码
     */
    private final MqttErrorCode errCode;

    public TopicAliasInvalidException(){
        this.errCode = MqttErrorCode.TOPIC_ALIAS_INVALID;
    }
}
