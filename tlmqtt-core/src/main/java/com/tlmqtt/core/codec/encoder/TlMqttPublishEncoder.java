package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttPublishEncoder extends AbstractTlMqttEncoder<TlMqttPublishReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPublishReq req, ByteBuf out, MqttMessageType mqttMessageType)  {

        log.info("encode publish req[{}]",req);
        TlMqttFixedHead fixedHead = req.getFixedHead();
        TlMqttPublishVariableHead variableHead = req.getVariableHead();
        int type = getFixedHeaderByte(fixedHead);
        MqttQoS qos = fixedHead.getQos();
        //消息类型
        out.writeByte(type);


        int remindLength = fixedHead.getLength();

        int propertiesLength = variableHead.getPropertiesLength();

        //消息发送到其他客户端的版本
        MqttVersion mqttVersion = req.getMqttVersion();

        if(mqttVersion==MqttVersion.MQTT_3_1_1){
            propertiesLength = 0;
        }

        //剩余长度
        writeVariableByteInteger(out, remindLength);
        String topic = variableHead.getTopic();
        byte[] topicBytes = topic.getBytes();
        out.writeShort(topicBytes.length);
        out.writeBytes(topicBytes);
        Long messageId = variableHead.getMessageId();
        if (qos != MqttQoS.AT_MOST_ONCE) {
            out.writeShort(messageId.intValue());
        }
        if(mqttVersion==MqttVersion.MQTT_5){
            writeVariableByteInteger(out, propertiesLength);
            writeProperties(variableHead, out);
        }
        Object content = req.getPayload().getContent();
        byte[] bytes = content.toString().getBytes();
        out.writeBytes(bytes);
        log.info("encode publish req[{}] finished",req);
    }
    private void writeProperties(TlMqttPublishVariableHead variableHead, ByteBuf out) {
        Boolean payloadFormatIndicator = variableHead.getPayloadFormatIndicator();

        //载荷格式指示
        if (payloadFormatIndicator != null) {
            out.writeByte(PropertiesCode.PAYLOAD_FORMAT_INDICATOR.getCode());
            out.writeByte(payloadFormatIndicator ? 1 : 0);
        }

        Integer messageExpiryInterval = variableHead.getMessageExpiryInterval();
        if(null != messageExpiryInterval){
            out.writeByte(PropertiesCode.MESSAGE_EXPIRY_INTERVAL.getCode());
            out.writeInt(messageExpiryInterval);
        }
        Integer topicAlias = variableHead.getTopicAlias();
        if(null != topicAlias){
            //一个字节标识符 + 2字节值
            out.writeByte(PropertiesCode.TOPIC_ALIAS.getCode());
            out.writeShort(topicAlias);
        }
        String responseTopic = variableHead.getResponseTopic();
        if(null != responseTopic){
            byte[] responseTopicBytes = responseTopic.getBytes(StandardCharsets.UTF_8);
            //一个字节标识符 + 2字节长度 + 字符串
            out.writeByte(PropertiesCode.RESPONSE_TOPIC.getCode());
            out.writeShort(responseTopicBytes.length);
            out.writeBytes(responseTopicBytes);
        }

        String correlationData = variableHead.getCorrelationData();
        if(null != correlationData){
            byte[] correlationDataBytes = correlationData.getBytes(StandardCharsets.UTF_8);
            //一个字节标识符 + 2字节长度 + 字符串
            out.writeByte(PropertiesCode.CORRELATION_DATA.getCode());
            out.writeShort(correlationDataBytes.length);
            out.writeBytes(correlationDataBytes);
        }

        List<UserProperty> userPropertyList = variableHead.getUserProperties();
        if (userPropertyList != null && !userPropertyList.isEmpty()) {
            for (UserProperty userProperty : userPropertyList) {
                byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                out.writeByte(PropertiesCode.USER_PROPERTY.getCode());
                out.writeShort(keyBytes.length);
                out.writeBytes(keyBytes);
                out.writeShort(valueBytes.length);
                out.writeBytes(valueBytes);
            }
        }

        Integer subscriptionIdentifier = variableHead.getSubscriptionIdentifier();
        if (subscriptionIdentifier != null) {
            // 1字节标识符 +变长字节
            out.writeByte(PropertiesCode.SUBSCRIPTION_IDENTIFIER.getCode());

            writeVariableByteInteger(out, subscriptionIdentifier);
        }

        String contentType = variableHead.getContentType();
        if (contentType != null) {
            byte[] contentTypeBytes = contentType.getBytes(StandardCharsets.UTF_8);
            out.writeByte(PropertiesCode.CONTENT_TYPE.getCode());
            // 1字节标识符 + 2字节长度 + 字符串
            out.writeShort(contentTypeBytes.length);
            out.writeBytes(contentTypeBytes);
        }
    }


    private int getFixedHeaderByte(TlMqttFixedHead header) {
        int ret = 0;
        ret |= header.getMessageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.getQos().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return ret;
    }
}
