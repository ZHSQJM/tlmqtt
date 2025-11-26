package com.tlmqtt.core.codec.decoder;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.variable.TlMqttUnSubscribeVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttUnSubscribeDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttUnSubscribeReq build(ByteBuf buf, int type,int remainingLength,TlMqttSession session) {

        TlMqttFixedHead fixedHead = decodeFixedHeader(type,remainingLength);
        TlMqttUnSubscribeVariableHead variableHead = decodeVariableHeader(buf,session);
        TlMqttUnSubscribePayload payload = decodePayload(buf);
        return TlMqttUnSubscribeReq.builder().fixedHead(fixedHead).variableHead(variableHead).payload(payload).build();
    }


    TlMqttFixedHead decodeFixedHeader(int type,int remainingLength) {
        return TlMqttFixedHead.builder().messageType(MqttMessageType.UNSUBSCRIBE)
            .length(remainingLength).build();
    }


    TlMqttUnSubscribeVariableHead decodeVariableHeader(ByteBuf buf, TlMqttSession session) {

        int messageId = buf.readUnsignedShort();
        TlMqttUnSubscribeVariableHead.TlMqttUnSubscribeVariableHeadBuilder builder = TlMqttUnSubscribeVariableHead.builder()
            .messageId(messageId);
        if(session.getMqttVersion() == MqttVersion.MQTT_5){
            int propertyLength = decodeRemainingLength(buf);
            // 4. 记录属性读取的起始位置
            final int propertiesStartIndex = buf.readerIndex();
            List<UserProperty> userProperties = new ArrayList<>();
            // 5. 循环读取属性直到达到属性长度
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                if (Objects.requireNonNull(propertiesCode) == PropertiesCode.USER_PROPERTY) {
                    int keyLength = buf.readShort();
                    byte[] key = new byte[keyLength];
                    buf.readBytes(key);
                    int valueLength = buf.readShort();
                    byte[] value = new byte[valueLength];
                    buf.readBytes(value);
                    UserProperty build = UserProperty.builder().key(new String(key)).value(new String(value)).build();
                    userProperties.add(build);
                }
                log.error("未知属性");
            }
            builder.userProperties(userProperties);
        }

        return builder.build();
    }


    TlMqttUnSubscribePayload decodePayload(ByteBuf buf) {
        TlMqttUnSubscribePayload payload = new TlMqttUnSubscribePayload();
        List<TlTopic> topics = new ArrayList<>();

        while (buf.readableBytes() != 0) {
            TlTopic topic = new TlTopic();
            int topicFilterLength = buf.readUnsignedShort();
            byte[] topicFilter = new byte[topicFilterLength];
            buf.readBytes(topicFilter);
            String topicFilterStr = new String(topicFilter);
            topic.setName(topicFilterStr);
            topics.add(topic);
        }
        payload.setTopics(topics);
        return payload;
    }
}
