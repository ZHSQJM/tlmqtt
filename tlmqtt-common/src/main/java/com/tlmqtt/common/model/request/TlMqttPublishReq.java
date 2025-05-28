package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import lombok.*;

/**
 * @Author: hszhou
 * @Date: 2024/11/26 13:23
 * @Description: 发布消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttPublishReq extends AbstractTlMessage  {


    private TlMqttFixedHead fixedHead;
    private TlMqttPublishVariableHead variableHead;
    private TlMqttPublishPayload payload;

    public static TlMqttPublishReq build(String topic,
                                            MqttQoS qoS,
                                            boolean retain,
                                            String content,
                                            Long messageId){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBLISH,qoS,retain);
        TlMqttPublishVariableHead variableHead = TlMqttPublishVariableHead.build(topic, messageId);
        TlMqttPublishPayload payload = TlMqttPublishPayload.build(content);
        return new TlMqttPublishReq(fixedHead,variableHead,payload);
    }

    public static TlMqttPublishReq build(String topic,
        MqttQoS qoS,
        String content,
        Long messageId,
        boolean isDup){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBLISH,qoS,isDup,false);
        TlMqttPublishVariableHead variableHead = TlMqttPublishVariableHead.build(topic, messageId);
        TlMqttPublishPayload payload = TlMqttPublishPayload.build(content);
        return new TlMqttPublishReq(fixedHead,variableHead,payload);
    }


    public static TlMqttPublishReq build(String topic,
        MqttQoS qoS,
        boolean retain,
        String content){
        TlMqttFixedHead fixedHead = TlMqttFixedHead.build(MqttMessageType.PUBLISH,qoS,retain);
        TlMqttPublishVariableHead variableHead = TlMqttPublishVariableHead.build(topic, 0L);
        TlMqttPublishPayload payload = TlMqttPublishPayload.build(content);
        return new TlMqttPublishReq(fixedHead,variableHead,payload);
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBLISH;
    }
}
