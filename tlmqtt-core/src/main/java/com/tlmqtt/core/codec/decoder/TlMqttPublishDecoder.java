package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttPublishDecoder extends AbstractTlMqttDecoder{

    @Override
    public TlMqttPublishReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session ) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(type, remainingLength);
        MqttQoS qos = fixedHead.getQos();
        TlMqttPublishVariableHead variableHead = decodeVariableHeader(buf, qos,session);
        TlMqttPublishPayload payload = decodePayload(buf);
        TlMqttPublishReq req = TlMqttPublishReq.builder().fixedHead(fixedHead).variableHead(variableHead)
            .payload(payload).build();
        //log.info("fixedHeade ==[{}],variableHead==[{}],payload==[{}],TlMqttPublishReq=[{}]",fixedHead,variableHead,payload,req);
        return req;
    }

    TlMqttFixedHead decodeFixedHeader(int type, int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        int messageType = type >> 4;
        int retain = (type) & 1;
        int qos = (type >> 1) & 3;
        int dup = (type >> 3) & 1;
        fixedHead.setMessageType(MqttMessageType.valueOf(messageType));
        fixedHead.setDup(dup != 0);
        fixedHead.setQos(MqttQoS.valueOf(qos));
        fixedHead.setRetain(retain != 0);
        fixedHead.setLength(remainingLength);


        return fixedHead;
    }


    TlMqttPublishVariableHead decodeVariableHeader(ByteBuf buf, MqttQoS qos, TlMqttSession session) {
        int topicLength = buf.readUnsignedShort();
        byte[] topic = new byte[topicLength];
        buf.readBytes(topic);
        //todo  不能包含通配符 如果存在通配符则不能发布 抛异常
        String topicName = new String(topic);
        TlMqttPublishVariableHead.TlMqttPublishVariableHeadBuilder builder = TlMqttPublishVariableHead.builder()
                                                                                                      .topic(topicName);
        if (qos != MqttQoS.AT_MOST_ONCE) {
            int messageId = buf.readUnsignedShort();
            builder.messageId((long)messageId);
        }
        if(session.getMqttVersion() == MqttVersion.MQTT_5){
            int propertyLength = decodeRemainingLength(buf);
            builder.propertiesLength(propertyLength);
            //log.info("获取到publish的属性长度为【{}】",propertyLength);
            // 4. 记录属性读取的起始位置
            final int propertiesStartIndex = buf.readerIndex();
            List<UserProperty> userProperties = new ArrayList<>();
            // 5. 循环读取属性直到达到属性长度
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                switch (Objects.requireNonNull(propertiesCode)) {
                    case PAYLOAD_FORMAT_INDICATOR:
                        int payloadFormatIndicator = buf.readByte();
                        builder.payloadFormatIndicator(payloadFormatIndicator==1);
                        break;
                    case MESSAGE_EXPIRY_INTERVAL:
                        int messageExpiryInterval = buf.readInt();
                        builder.messageExpiryInterval(messageExpiryInterval);
                        break;
                    case TOPIC_ALIAS:
                        // 主题别名
                        int topicAlias = buf.readShort();
                        builder.topicAlias(topicAlias);
                        break;
                    case RESPONSE_TOPIC:
                        int responseTopicLength = buf.readShort();
                        byte[]responseTopicByte = new byte[responseTopicLength];
                        buf.readBytes(responseTopicByte);
                        String responseTopic = new String(responseTopicByte);
                        builder.responseTopic(responseTopic);
                        break;
                    case CORRELATION_DATA:
                         int correlationDataLength = buf.readShort();
                         byte[]correlationDataByte = new byte[correlationDataLength];
                         buf.readBytes(correlationDataByte);
                         String correlationData = new String(correlationDataByte);
                         builder.correlationData(correlationData);
                         break;
                    case USER_PROPERTY:
                        int keyLength = buf.readShort();
                        byte[] keyByte = new byte[keyLength];
                        buf.readBytes(keyByte);
                        String key = new String(keyByte);
                        int valueLength = buf.readShort();
                        byte[] valueByte = new byte[valueLength];
                        buf.readBytes(valueByte);
                        String value = new String(valueByte);
                        UserProperty userProperty = UserProperty.builder().key(key).value(value).build();
                        userProperties.add(userProperty);
                        break;
                    case SUBSCRIPTION_IDENTIFIER:
                        //订阅标识符
                        int subscriptionIdentifier=decodeRemainingLength(buf);
                        if(subscriptionIdentifier==0){
                            //throw new TlProtocolVersionException(MqttVersion.MQTT_5);
                        }
                        builder.subscriptionIdentifier(subscriptionIdentifier);
                        break;
                    case CONTENT_TYPE:
                        log.info("==内容类型【{}】",propertyIdentifier);
                        //内容类型
                        int contentTypeLength = buf.readShort();
                        byte[] contentTypeByte = new byte[contentTypeLength];
                        buf.readBytes(contentTypeByte);
                        String contentType = new String(contentTypeByte);
                        builder.contentType(contentType);
                        break;
                    default:
                        log.info("未知属性【{}】",propertyIdentifier);
                        break;

                }
                builder.userProperties(userProperties);
            }
        }

        return builder.build();


    }


    TlMqttPublishPayload decodePayload(ByteBuf buf) {
        int contentLength = buf.readableBytes();
        byte[] contentByte = new byte[contentLength];
        buf.readBytes(contentByte);
        String content = new String(contentByte);
        return TlMqttPublishPayload.builder().content(content).build();
    }

}
