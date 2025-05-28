package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:07
 * @Description: 接收客户端的心跳
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttHeartBeatReq extends AbstractTlMessage {
    private TlMqttFixedHead fixedHead;

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PINGRESP;
    }
}
