package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@Accessors
@SuperBuilder
public class TlMqttPublishReq extends AbstractTlMessage  {

    private TlMqttPublishVariableHead variableHead;

    private TlMqttPublishPayload payload;

    private MqttVersion mqttVersion;


    public static TlMqttPublishReq build(TlMqttFixedHead fixedHead,
                                         TlMqttPublishVariableHead variableHead,
                                         TlMqttPublishPayload payload,
                                         MqttVersion mqttVersion){

        int remainingLength = 0;
        String topic = variableHead.getTopic();
        remainingLength += Short.BYTES+topic.getBytes(StandardCharsets.UTF_8).length;
        MqttQoS qos = fixedHead.getQos();
        if (qos != MqttQoS.AT_MOST_ONCE) {
            remainingLength += Short.BYTES;
        }

        // 只有在MQTT 5.0时才计算属性长度
        if(mqttVersion == MqttVersion.MQTT_5){
            int propertiesLength = calculatePropertiesLength(variableHead);
            remainingLength += calculateVariableByteIntegerLength(propertiesLength) + propertiesLength;
            variableHead.setPropertiesLength(propertiesLength);
        }


        byte[] content = payload.getContent().toString().getBytes(StandardCharsets.UTF_8);
        int contentLength = content.length;
        remainingLength +=contentLength;
        fixedHead.setLength(remainingLength);

       // log.info("remainingLength=【{}】",remainingLength);
        return TlMqttPublishReq.builder()
            .variableHead(variableHead)
            .fixedHead(fixedHead)
            .payload(payload)
            .mqttVersion(mqttVersion)
            .build();
    }


    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.PUBLISH;
    }


    /**
     * 计算属性部分长度
     *
     * @param variableHead 可变头
     * @return 属性长度
     */
    private static int calculatePropertiesLength(TlMqttPublishVariableHead variableHead) {
        int propertiesLength = 0;

        if (variableHead.getPayloadFormatIndicator() != null) {
            propertiesLength += 2;
        }

        // 消息过期时间
        if (variableHead.getMessageExpiryInterval() != null) {
            propertiesLength += 5;
        }



        // 主题别名
        if (variableHead.getTopicAlias() != null) {
            propertiesLength += 3;
        }

        // 响应主题
        if (variableHead.getResponseTopic() != null) {
            byte[] getResponseTopicBytes = variableHead.getResponseTopic().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + getResponseTopicBytes.length;
        }

        // 对比数据
        if (variableHead.getCorrelationData() != null) {
            byte[] correlationDataBytes = variableHead.getCorrelationData().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + correlationDataBytes.length;
        }



        // 用户属性 (可选) // 1字节标识 + 2字节key长度 + key + 2字节value长度 + value
        List<UserProperty> userProperties = variableHead.getUserProperties();
        if (userProperties != null && !userProperties.isEmpty()) {
            for (UserProperty userProperty : userProperties) {
                byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                propertiesLength += 3 + keyBytes.length + 2 + valueBytes.length;
            }
        }

        // 订阅标识符
        if (variableHead.getSubscriptionIdentifier() != null) {
            int i = calculateVariableByteIntegerLength(variableHead.getSubscriptionIdentifier());
            propertiesLength += 1 + i;
        }

        // 响应信息 (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getContentType() != null) {
            byte[] contentTypeBytes = variableHead.getContentType().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + contentTypeBytes.length;
        }

        return propertiesLength;
    }
}
