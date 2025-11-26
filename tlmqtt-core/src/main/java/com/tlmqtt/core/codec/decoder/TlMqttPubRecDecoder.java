package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.enums.PubReasonCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubRecReq;
import com.tlmqtt.common.model.variable.TlMqttPubRecVariableHead;
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
public class TlMqttPubRecDecoder extends AbstractTlMqttDecoder {

    @Override
    public TlMqttPubRecReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session) {


        TlMqttFixedHead fixedHead = decodeFixedHeader(type,remainingLength);
        TlMqttPubRecVariableHead variableHead = decodeVariableHeader(buf, session);
        return TlMqttPubRecReq.builder().fixedHead(fixedHead).variableHead(variableHead).build();


    }

    TlMqttFixedHead decodeFixedHeader(int type,int remainingLength) {
        return TlMqttFixedHead.builder().messageType(MqttMessageType.PUBREC)
            .length(remainingLength).build();
    }

    TlMqttPubRecVariableHead decodeVariableHeader(ByteBuf buf,TlMqttSession session) {
        int messageId = buf.readUnsignedShort();
        TlMqttPubRecVariableHead.TlMqttPubRecVariableHeadBuilder builder = TlMqttPubRecVariableHead.builder()
            .messageId((long) messageId);
        if (session.getMqttVersion()== MqttVersion.MQTT_5) {
            //原因码
            byte reasonCode = buf.readByte();
            builder.reasonCode(reasonCode);
            //当原因码为0x00的时候 且没有属性时，原因码和属性长度可以被省略 mqttx客户端就是省略了
            if(reasonCode == PubReasonCode.SUCCESS.getCode()&&!buf.isReadable()){
                return builder.build();
            }
            int propertyLength = decodeRemainingLength(buf);

            final int propertiesStartIndex = buf.readerIndex();
            // 当读指针的值减去属性开始的索引值小于属性长度时，继续读取属性

            List<UserProperty> userProperties = new ArrayList<>();
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                switch (Objects.requireNonNull(propertiesCode)) {
                    case PAYLOAD_FORMAT_INDICATOR:
                        short reasonByteLength = buf.readShort();
                        byte[] reasonByte = new byte[reasonByteLength];
                        buf.readBytes(reasonByte);
                        String reasonStr = new String(reasonByte);
                        builder.reasonString(reasonStr);
                        break;
                    case USER_PROPERTY:
                        int keyLength = buf.readShort();
                        byte[] keyPropertyByte = new byte[keyLength];
                        buf.readBytes(keyPropertyByte);
                        String key = new String(keyPropertyByte);

                        int valueLength = buf.readShort();
                        byte[] valuePropertyByte = new byte[valueLength];
                        buf.readBytes(valuePropertyByte);
                        String value = new String(valuePropertyByte);
                        UserProperty userProperty = UserProperty.builder().key(key).value(value).build();
                        userProperties.add(userProperty);
                        builder.userPropertyList(userProperties);
                        break;
                    default:
                        log.error("未知属性");
                        break;
                }
            }
        }
        return builder.build();
    }

}
