package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.variable.TlMqttDisconnectVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttDisConnectDecoder  extends AbstractTlMqttDecoder{

    public TlMqttDisConnectDecoder(MqttConfiguration configuration){
        super(configuration);
    }

    @Override
    public TlMqttDisconnectReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session){
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttDisconnectVariableHead variableHead = decodeVariableHeader(buf,session);
        return TlMqttDisconnectReq.builder()
            .fixedHead(fixedHead)
            .variableHead(variableHead)
            .build();

    }

    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        return TlMqttFixedHead.builder()
            .messageType(MqttMessageType.DISCONNECT)
            .length(remainingLength).build();
    }

    TlMqttDisconnectVariableHead decodeVariableHeader(ByteBuf buf,TlMqttSession session) {
        TlMqttDisconnectVariableHead.TlMqttDisconnectVariableHeadBuilder builder = TlMqttDisconnectVariableHead.builder();
        if(session.isVersion5()){
            byte reasonCode = buf.readByte();
            builder.reasonCode(reasonCode);
            int propertyLength = decodeRemainingLength(buf);
            // 4. 记录属性读取的起始位置
            final int propertiesStartIndex = buf.readerIndex();
            List<UserProperty> userProperties = new ArrayList<>();
            // 5. 循环读取属性直到达到属性长度
            while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
                byte propertyIdentifier = buf.readByte();
                PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
                switch (Objects.requireNonNull(propertiesCode)) {

                    case SESSION_EXPIRY_INTERVAL:
                        int sessionExpiryInterval = buf.readInt();
                        builder.sessionExpiryInterval(sessionExpiryInterval);
                        log.info("sessionExpiryInterval【{}】",sessionExpiryInterval);
                        break;
                    case REASON_STRING:
                        int reasonStringLength = buf.readShort();
                        byte[] reasonStringByte = new byte[reasonStringLength];
                        buf.readBytes(reasonStringByte);
                        builder.reasonString(new String(reasonStringByte));
                        log.info("reasonString【{}】",new String(reasonStringByte));
                        break;
                    case USER_PROPERTY:
                        int keyLength = buf.readShort();
                        byte[] key = new byte[keyLength];
                        buf.readBytes(key);
                        int valueLength = buf.readShort();
                        byte[] value = new byte[valueLength];
                        buf.readBytes(value);
                        UserProperty userProperty = UserProperty.builder().key(new String(key)).value(new String(value))
                            .build();
                        log.info("userProperty【{}】",userProperty);
                        userProperties.add(userProperty);
                    case SERVER_REFERENCE:
                        int serverReferenceLength = buf.readShort();
                        byte[] serverReference = new byte[serverReferenceLength];
                        buf.readBytes(serverReference);
                        builder.serverReference(new String(serverReference));
                        log.info("serverReference【{}】",new String(serverReference));
                        break;
                    default:
                        log.error("未知属性");
                        break;
                }
            }
            builder.userProperties(userProperties);
        }

        return builder.build();
    }
}
