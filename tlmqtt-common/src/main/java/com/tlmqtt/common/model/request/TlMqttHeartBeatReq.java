package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Accessors
@SuperBuilder
public class TlMqttHeartBeatReq extends AbstractTlMessage {


    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PINGREQ;
    }
}
