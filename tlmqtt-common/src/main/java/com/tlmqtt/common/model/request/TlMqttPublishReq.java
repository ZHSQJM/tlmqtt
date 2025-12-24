package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import io.netty.buffer.ByteBuf;
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
@SuperBuilder
public class TlMqttPublishReq extends AbstractTlMessage  {

    /**
     * 可变头
     */
    private TlMqttPublishVariableHead variableHead;

    /**
     * 固定头
     */
    private TlMqttPublishPayload payload;

    /**
     * MQTT版本
     */
    private MqttVersion mqttVersion;

    /**
     * 服务端接收到次消息的时间
     */
    private Long acceptTime;


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

    public TlMqttPublishReq copy() {
        // 1. 复制 Fixed Head
        TlMqttFixedHead newFixedHead = TlMqttFixedHead.builder()
            .messageType(this.getFixedHead().getMessageType())
            .dup(this.getFixedHead().isDup())
            .qos(this.getFixedHead().getQos())
            .retain(this.getFixedHead().isRetain())
            .length(this.getFixedHead().getLength())
            .build();

        // 2. 复制 Variable Head
        TlMqttPublishVariableHead newVariableHead = TlMqttPublishVariableHead.builder()
            .topic(this.variableHead.getTopic())
            .messageId(this.variableHead.getMessageId())
            .userProperties(this.variableHead.getUserProperties()) // 注意：Properties 如果包含复杂对象也需深拷
            .build();

        // 3. 复制 Payload (最关键：处理 ByteBuf)
        TlMqttPublishPayload newPayload = null;
        if (this.payload != null && this.payload.getContent() != null) {
            Object content = this.payload.getContent();
            if (content instanceof ByteBuf) {
                // 使用 copy() 产生一块完全独立的堆外内存
                ByteBuf originalBuf = (ByteBuf) content;
                newPayload = new TlMqttPublishPayload(originalBuf.copy());
            } else {
                // 如果是 String 或 byte[]，直接赋值即可（String 是不可变的）
                newPayload = new TlMqttPublishPayload(content);
            }
        }

        // 4. 组装新请求对象
        return TlMqttPublishReq.builder()
            .fixedHead(newFixedHead)
            .variableHead(newVariableHead)
            .payload(newPayload)
            .acceptTime(this.acceptTime)
            .mqttVersion(this.mqttVersion)
            .build();
    }
}
