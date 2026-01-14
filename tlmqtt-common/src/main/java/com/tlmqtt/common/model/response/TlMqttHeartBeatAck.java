package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@SuperBuilder
public class TlMqttHeartBeatAck extends AbstractTlMessage {



    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PINGRESP;
    }
}
