package com.tlmqtt.core.codec.decoder;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttSubscribeReq;
import com.tlmqtt.common.model.variable.TlMqttSubscribeVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttSubscribeDecoder extends AbstractTlMqttDecoder {


    @Override
    public TlMqttSubscribeReq build(ByteBuf buf, int type, int remainingLength,TlMqttSession session) {

        TlMqttFixedHead fixedHead = decodeFixedHeader(type,remainingLength);
        TlMqttSubscribeVariableHead variableHead = decodeVariableHeader(buf,session);
        TlMqttSubscribePayload payload = decodePayLoad(buf,session);
        return TlMqttSubscribeReq.builder().fixedHead(fixedHead).variableHead(variableHead).payload(payload).build();

    }

    TlMqttFixedHead decodeFixedHeader(int type,int remainingLength) {


        return TlMqttFixedHead.builder().messageType(MqttMessageType.SUBSCRIBE)
            .length(remainingLength).build();
    }


    TlMqttSubscribeVariableHead decodeVariableHeader(ByteBuf buf, TlMqttSession session) {
        int messageId = buf.readUnsignedShort();
        TlMqttSubscribeVariableHead.TlMqttSubscribeVariableHeadBuilder builder = TlMqttSubscribeVariableHead.builder()
            .messageId(messageId);

        if(session.getMqttVersion()==MqttVersion.MQTT_5){
            int propertyLength = decodeRemainingLength(buf);
            // 4. 记录属性读取的起始位置
            final int propertiesStartIndex = buf.readerIndex();
            List<UserProperty> userProperties = new ArrayList<>();
            // 5. 循环读取属性直到达到属性长度
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                switch (Objects.requireNonNull(propertiesCode)) {
                    case SUBSCRIPTION_IDENTIFIER:
                        //如果服务端在不支持订阅标识符（Subscription Identifier）的情况下收到了包含订阅标识符的SUBSCRIBE报文，将造成协议错误（Protocol Error）。此时服务端将发送包含原因码为0xA1（订阅标识符不支持）的DISCONNECT报文，如4.13节所述。
                        if( !Constant.SUBSCRIPTION_IDENTIFIERS_AVAILABLE){
                            throw new TlProtocolErrorException( MqttErrorCode.SUBSCRIPTION_IDENTIFIERS_AVAILABLE,MqttMessageType.SUBSCRIBE);
                        }
                        int subscriptionIdentifier = decodeRemainingLength(buf);
                        builder.subscriptionIdentifier(subscriptionIdentifier);
                        break;
                    case USER_PROPERTY:
                        int keyLength = buf.readShort();
                        byte[] key = new byte[keyLength];
                        buf.readBytes(key);
                        int valueLength = buf.readShort();
                        byte[] value = new byte[valueLength];
                        buf.readBytes(value);
                        UserProperty build = UserProperty.builder().key(new String(key)).value(new String(value))
                            .build();
                        userProperties.add(build);
                    default:
                        log.error("未知属性");
                        break;
                }
            }
            builder.userPropertyList(userProperties);
        }
        return builder.build();
    }


    TlMqttSubscribePayload decodePayLoad(ByteBuf buf,TlMqttSession session) {

        int i = buf.readableBytes();
        if (i == 0) {
            return null;
        }
        TlMqttSubscribePayload payload = new TlMqttSubscribePayload();
        List<TlTopic> topics = new ArrayList<>();
        while (buf.readableBytes() != 0) {
            TlTopic topic = new TlTopic();
            int topicFilterLength = buf.readUnsignedShort();
            byte[] topicFilter = new byte[topicFilterLength];
            buf.readBytes(topicFilter);
            String topicFilterStr = new String(topicFilter);
            topic.setName(topicFilterStr);
            if(session.getMqttVersion()==MqttVersion.MQTT_5){

                log.info("订阅的是【{}】",topicFilterStr);
                //如果服务端在不支持通配符订阅（Wildcard Subscription）的情况下收到了包含通配符订阅的SUBSCRIBE报文，将造成协议错误（Protocol Error）。此时服务端将发送包含原因码为0xA2（通配符订阅不支持）的DISCONNECT报文，如4.13节所述。
                if((topicFilterStr.contains(Constant.ASTERISK) || topicFilterStr.contains(Constant.TOPIC_SPLITTER)) && !Constant.WILDCARD_SUBSCRIPTION_AVAILABLE){
                    throw new TlProtocolErrorException(MqttErrorCode.WILDCARD_SUBSCRIPTION_AVAILABLE,MqttMessageType.SUBSCRIBE);
                }
                //如果服务端在不支持共享订阅（Shared Subscription）的情况下收到了包含共享订阅的SUBSCRIBE报文，将造成协议错误（Protocol Error）。此时服务端将发送包含原因码为0x9E（共享订阅不支持）的DISCONNECT报文
                if(topicFilterStr.startsWith(Constant.SHARE_PREFIX_SUBSCRIBE) && !Constant.SHARED_SUBSCRIPTION_AVAILABLE){
                    throw new TlProtocolErrorException(MqttErrorCode.SHARED_SUBSCRIPTION_AVAILABLE,MqttMessageType.SUBSCRIBE);
                }

                /* 订阅选项的第0和1比特代表最大服务质量字段。此字段给出服务端可以向此客户端发送的应用消息的最大QoS等级。最大服务质量字段为3将造成协议错误（Protocol Error）。
                 订阅选项的第2比特表示非本地（No Local）选项。值为1，表示应用消息不能被转发给发布此消息的客户标识符 [MQTT-3.8.3-3]。共享订阅时把非本地选项设为1将造成协议错误（Protocol Error） [MQTT-3.8.3-4]。
                 订阅选项的第3比特表示发布保留（Retain As Published）选项。值为1，表示向此订阅转发应用消息时保持消息被发布时设置的保留（RETAIN）标志。值为0，表示向此订阅转发应用消息时把保留标志设置为0。当订阅建立之后，发送保留消息时保留标志设置为1。
                 订阅选项的第4和5比特表示保留操作（Retain Handling）选项。此选项指示当订阅建立时，是否发送保留消息。此选项不影响之后的任何保留消息的发送。如果没有匹配主题过滤器的保留消息，则此选项所有值的行为都一样。值可以设置为：
                 0 = 订阅建立时发送保留消息
                 1 = 订阅建立时，若该订阅当前不存在则发送保留消息
                 2 = 订阅建立时不要发送保留消息
                 保留操作的值设置为3将造成协议错误（Protocol Error）。

                订阅选项的第6和7比特为将来所保留。服务端必须把此保留位非0的SUBSCRIBE报文当做无效报文 [MQTT-3.8.3-5]。*/

                byte subscriptionOptions = buf.readByte();
                int maxQos = subscriptionOptions & 0x03;
                if (maxQos == 3) {
                 // throw new TlProtocolErrorException(MqttErrorCode);
                }
                int retainHandling = (subscriptionOptions >> 4) & 0x03;
                if (retainHandling == 3) {
                  //throw new TlProtocolErrorException("Invalid retainHandling=3 in SUBSCRIBE packet");
                }

                topic.setQos(maxQos);
                //todo 如果是共享订阅 那么这个noLocal不能为1
                topic.setNoLocal((subscriptionOptions & 0x04) != 0);
                topic.setRetainAsPublished((subscriptionOptions & 0x08) != 0);
                topic.setRetainHandling(retainHandling);
                log.info("订阅最大maxQos【{}】,noLocal【{}】,retainAsPublished【{}】,retainHanding【{}】",maxQos,topic.getNoLocal(),topic.getRetainAsPublished(),topic.getRetainHandling());

            }else if(session.getMqttVersion()==MqttVersion.MQTT_3_1_1){
                 short qos = buf.readUnsignedByte();
                 topic.setQos((int) qos);
            }
            topics.add(topic);
        }
        payload.setTopics(topics);
        return payload;
    }
}
