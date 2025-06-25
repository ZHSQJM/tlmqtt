package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlMqttHeartBeat  {

    private TlMqttFixedHead fixedHead;


    public static TlMqttHeartBeat of(MqttMessageType messageType){
        TlMqttHeartBeat res = new TlMqttHeartBeat();
        TlMqttFixedHead fixedHead =TlMqttFixedHead.build(messageType);
        res.setFixedHead(fixedHead);
        return res;
    }
}
