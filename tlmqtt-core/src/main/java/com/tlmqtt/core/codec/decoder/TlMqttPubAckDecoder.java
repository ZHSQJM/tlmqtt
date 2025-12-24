package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.enums.PubReasonCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPubAckReq;
import com.tlmqtt.common.model.variable.TlMqttPubAckVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttPubAckDecoder extends AbstractTlMqttDecoder {


    public TlMqttPubAckDecoder(MqttConfiguration configuration){
        super(configuration);
    }

    @Override
    public TlMqttPubAckReq build(ByteBuf buf, int type,int remainingLength, TlMqttSession session) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttPubAckVariableHead variableHead = decodeVariableHeader(buf,session);
        return TlMqttPubAckReq.builder()
            .fixedHead(fixedHead).variableHead(variableHead).build();

    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        return TlMqttFixedHead.builder()
            .messageType(MqttMessageType.PUBACK)
            .length(remainingLength).build();
    }

    TlMqttPubAckVariableHead decodeVariableHeader(ByteBuf buf,TlMqttSession session) {
        int messageId = buf.readUnsignedShort();
        TlMqttPubAckVariableHead.TlMqttPubAckVariableHeadBuilder builder = TlMqttPubAckVariableHead.builder()
            .messageId((long) messageId);
        if(session.isVersion5()){
            //原因码
            byte reasonCode = buf.readByte();
            builder.reasonCode(reasonCode);
            //当原因码为0x00的时候 且没有属性时，原因码和属性长度可以被省略 mqttx客户端就是省略了
            if(reasonCode == PubReasonCode.SUCCESS.getCode()&&!buf.isReadable()){
                return builder.build();
            }
            int propertyLength = decodeRemainingLength(buf);
            final int propertiesStartIndex = buf.readerIndex();
            List<UserProperty> userProperties = new ArrayList<>();
            // 5. 循环读取属性直到达到属性长度
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                switch (Objects.requireNonNull(propertiesCode)) {
                    case REASON_STRING:
                        int reasonStrLength = buf.readShort();
                        byte[] reasonStrByte = new byte[reasonStrLength];
                        buf.readBytes(reasonStrByte);
                        String reasonStr = new String(reasonStrByte);
                        builder.reasonString(reasonStr);
                        break;
                    case USER_PROPERTY:
                        int keyLength = buf.readShort();
                        byte[] key = new byte[keyLength];
                        buf.readBytes(key);
                        int valueLength = buf.readShort();
                        byte[] value = new byte[valueLength];
                        buf.readBytes(value);
                        UserProperty userProperty = UserProperty.builder().key(new String(key)).value(new String(value)).build();
                        userProperties.add(userProperty);
                        builder.userPropertyList(userProperties);
                        break;
                    default:
                        break;
                    }
                }

        }
        return builder.build();
    }


}
