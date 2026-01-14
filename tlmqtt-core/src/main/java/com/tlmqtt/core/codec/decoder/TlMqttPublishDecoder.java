package com.tlmqtt.core.codec.decoder;

import cn.hutool.core.util.StrUtil;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.exception.TlMalformedPacketException;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.exception.TlProtocolErrorException;
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
    private  int maximumQos;
    private  boolean retainAvailable;
    private  List<String> invalidTopicNames;
    private int topicAliasMaximum;


    public  TlMqttPublishDecoder(MqttConfiguration configuration){
        super(configuration);
        this.maximumQos = configuration.getInt(MqttConfiguration.MAXIMUM_QOS);
        this.retainAvailable = configuration.getBoolean(MqttConfiguration.RETAIN_AVAILABLE);
        this.invalidTopicNames = configuration.getList(MqttConfiguration.INVALID_TOPIC_NAMES);
        this.topicAliasMaximum = configuration.getInt(MqttConfiguration.TOPIC_ALIAS_MAXIMUM);
        configuration.addListener(property -> {
            if (property == MqttConfiguration.Property.MAXIMUM_QOS) {
                int newValue = configuration.getInt(MqttConfiguration.Property.MAXIMUM_QOS.getKey());
                log.debug("【TLMQTT】Codec maximumQos hot-updated to: {}", newValue);
                this.maximumQos = newValue;
            }
            if (property == MqttConfiguration.Property.RETAIN_AVAILABLE) {
                boolean newValue = configuration.getBoolean(MqttConfiguration.Property.RETAIN_AVAILABLE.getKey());
                log.debug("【TLMQTT】Codec retainAvailable hot-updated to: {}", newValue);
                this.retainAvailable = newValue;
            }
            if (property == MqttConfiguration.Property.INVALID_TOPIC_NAMES) {
                List<String> newValue = configuration.getList(MqttConfiguration.Property.INVALID_TOPIC_NAMES.getKey());
                log.debug("【TLMQTT】Codec invalidTopicNames hot-updated to: {}", newValue);
                this.invalidTopicNames = newValue;
            }
            if (property == MqttConfiguration.Property.TOPIC_ALIAS_MAXIMUM) {
                int  newValue = configuration.getInt(MqttConfiguration.Property.TOPIC_ALIAS_MAXIMUM.getKey());
                log.debug("【TLMQTT】Codec topicAliasMaximum hot-updated to: {}", newValue);
                this.topicAliasMaximum = newValue;
            }
        });
    }

    @Override
    public TlMqttPublishReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session) {

        MqttVersion mqttVersion = session.getMqttVersion();
        TlMqttFixedHead fixedHead = decodeFixedHeader(type, remainingLength,mqttVersion);
        MqttQoS qos = fixedHead.getQos();
        TlMqttPublishVariableHead variableHead = decodeVariableHeader(buf, qos,session);
        TlMqttPublishPayload payload = decodePayload(buf);
        TlMqttPublishReq.TlMqttPublishReqBuilder<?, ?> builder = TlMqttPublishReq.builder().fixedHead(fixedHead)
            .variableHead(variableHead).payload(payload);
        if(session.isVersion5()){
            builder.acceptTime(getCurrentTime());
        }
        return builder.build();
    }


    /**
     * 解码固定报头
     * @param type 类型
     * @param remainingLength 剩余长度
     * @param mqttVersion mqtt版本
     * @return 固定报头
     */
    TlMqttFixedHead decodeFixedHeader(int type, int remainingLength,MqttVersion mqttVersion ) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        int messageType = type >> 4;
        int retain = (type) & 1;
        int qos = (type >> 1) & 3;
        //接收到超过其指定的最大服务质量的PUBLISH报文将造成协议错误（Protocol Error）。这种情况下应使用包含原因码为0x9B（不支持的QoS等级）的DISCONNECT报文进行处理，如4.13节所述。
        if(maximumQos<qos && mqttVersion == MqttVersion.MQTT_5){
            throw new TlMqttException(MqttErrorCode.CONNECTION_REFUSED_QOS_NOT_SUPPORTED,true,MqttMessageType.PUBLISH,null, MqttMessageType.DISCONNECT);
        }
        int dup = (type >> 3) & 1;
        fixedHead.setMessageType(MqttMessageType.valueOf(messageType));
        fixedHead.setDup(dup != 0);
        fixedHead.setQos(MqttQoS.valueOf(qos));
        fixedHead.setRetain(retain != 0);
        //如果服务端发送给客户端的CONNACK报文中包含保留可用属性，且属性值为0，但收到的PUBLISH报文中保留标志位为1，服务端使用包含原因码为0x9A（保留不支持）的DISCONNECT报文断开网络连接，如4.13节所述。
        if(!retainAvailable && retain!=0){
            throw new TlMqttException(MqttErrorCode.CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED,true,MqttMessageType.PUBLISH,null, MqttMessageType.DISCONNECT);
        }
        fixedHead.setLength(remainingLength);

        return fixedHead;
    }


    /**
     * 解码可变报头
     * @param buf 数据流
     * @param qos QoS等级
     * @param session 会话
     * @return 可变报头
     */
    TlMqttPublishVariableHead decodeVariableHeader(ByteBuf buf, MqttQoS qos, TlMqttSession session) {
        int topicLength = buf.readUnsignedShort();
        byte[] topic = new byte[topicLength];
        buf.readBytes(topic);
        //不能包含通配符 如果存在通配符则不能发布 抛异常
        String topicName = new String(topic);
        boolean correctTopic = StrUtil.isNotEmpty(topicName) && (topicName.contains(Constant.TOPIC_SPLITTER) || topicName.contains(
            Constant.ASTERISK));
        if(correctTopic){
           throw new TlMalformedPacketException(MqttMessageType.PUBLISH,MqttMessageType.DISCONNECT);
        }

        TlMqttPublishVariableHead.TlMqttPublishVariableHeadBuilder builder = TlMqttPublishVariableHead.builder().topic(topicName);
        if (qos != MqttQoS.AT_MOST_ONCE) {
            int messageId = buf.readUnsignedShort();
            builder.messageId((long)messageId);
            if (invalidTopicNames.contains(topicName)) {
                throw new TlMqttException(MqttErrorCode.TOPIC_NAME_INVALID, false, MqttMessageType.PUBLISH,(long)messageId,
                    qos == MqttQoS.AT_LEAST_ONCE ? MqttMessageType.PUBACK : MqttMessageType.PUBREC);
            }
        }

        if(session.isVersion5()){
            int propertyLength = decodeRemainingLength(buf);
            builder.propertiesLength(propertyLength);
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
                        //1) 主题别名为0或大于最大主题别名（Maximum Topic Alias），将造成协议错误（Protocol Error），接收端使用包含原因码为0x94（主题别名无效）的DISCONNECT报文断开网络连接
                        if(topicAlias == 0 || topicAlias> topicAliasMaximum){
                            throw new TlProtocolErrorException(MqttErrorCode.TOPIC_ALIAS_INVALID,MqttMessageType.PUBLISH,MqttMessageType.DISCONNECT);
                        }
                        builder.topicAlias(topicAlias);
                        break;
                    case RESPONSE_TOPIC:
                        int responseTopicLength = buf.readShort();
                        byte[]responseTopicByte = new byte[responseTopicLength];
                        buf.readBytes(responseTopicByte);
                        String responseTopic = new String(responseTopicByte);
                        if(responseTopic.contains(Constant.TOPIC_SPLITTER) || responseTopic.contains(Constant.TOPIC_WILDCARD)){
                            throw new TlProtocolErrorException(MqttMessageType.PUBLISH,MqttMessageType.DISCONNECT);
                        }
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
                            throw new TlProtocolErrorException(MqttMessageType.PUBLISH,MqttMessageType.DISCONNECT);
                        }
                        builder.subscriptionIdentifier(subscriptionIdentifier);
                        break;
                    case CONTENT_TYPE:
                        //内容类型
                        int contentTypeLength = buf.readShort();
                        byte[] contentTypeByte = new byte[contentTypeLength];
                        buf.readBytes(contentTypeByte);
                        String contentType = new String(contentTypeByte);
                        builder.contentType(contentType);
                        break;
                    default:
                        break;

                }
                builder.userProperties(userProperties);
            }
        }

        TlMqttPublishVariableHead publishVariableHead = builder.build();
        //主题名长度为0且没有主题别名，将造成协议错误（Protocol Error）。
        if(topicLength==0 && publishVariableHead.getTopicAlias()==null){
            throw new TlProtocolErrorException(MqttErrorCode.PROTOCOL_ERROR,MqttMessageType.PUBLISH);
        }

        return publishVariableHead;


    }


    TlMqttPublishPayload decodePayload(ByteBuf buf) {
        int contentLength = buf.readableBytes();
        byte[] contentByte = new byte[contentLength];
        buf.readBytes(contentByte);
        String content = new String(contentByte);
        return TlMqttPublishPayload.builder().content(content).build();
    }

}
