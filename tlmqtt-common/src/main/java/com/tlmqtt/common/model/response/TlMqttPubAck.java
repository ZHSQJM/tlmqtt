package com.tlmqtt.common.model.response;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Accessors
@SuperBuilder
public class TlMqttPubAck  extends AbstractTlMessage {

    private TlMqttPubAckVariableHead variableHead;

    public static TlMqttPubAck build(Long messageId,byte reasonCode,String reasonString, List<UserProperty> userProperties, MqttVersion version){
        //最少2个字节的消息标识符
        int remainingLength = 2;
        //属性长度为0
        int propertiesLength = 0;
        if(version == MqttVersion.MQTT_5) {
            //加上原因码
            remainingLength += 1;
            if (reasonString != null) {
                //加上一个字节的原因字符串的标识符 和2个字节的原因码长度 + 原因码的字节长度
                propertiesLength += +1 + 2 + reasonString.getBytes(StandardCharsets.UTF_8).length;
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                for (UserProperty userProperty : userProperties) {
                    byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                    byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                    // 1字节标识 + 2字节key长度 + key + 2字节value长度 + value
                    propertiesLength += 1 + 2 + keyBytes.length + 2 + valueBytes.length;
                }
            }
            if (propertiesLength > 0) {
                remainingLength += calculateVariableByteIntegerLength(propertiesLength) + propertiesLength;
            } else {
                //如果属性长度为0 也要写入
                remainingLength += 1;
            }
        }
        TlMqttFixedHead fixedHead = TlMqttFixedHead.builder()
                                                   .messageType(MqttMessageType.PUBACK)
                                                   .length(remainingLength).build();
        TlMqttPubAckVariableHead variableHead = TlMqttPubAckVariableHead.builder()
                                                                        .messageId(messageId)
                                                                        .reasonCode(reasonCode)
                                                                        .reasonString(reasonString)
                                                                        .userPropertyList(userProperties)
                                                                        .propertiesLength(propertiesLength)
                                                                        .build();
        return TlMqttPubAck.builder().fixedHead(fixedHead).variableHead(variableHead).build();
    }



    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBACK;
    }
}
