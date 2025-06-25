package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import lombok.*;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttHeartBeatReq extends AbstractTlMessage {
    private TlMqttFixedHead fixedHead;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PINGREQ;
    }
}
