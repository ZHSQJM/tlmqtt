package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.request.TlMqttPubCompReq;
import com.tlmqtt.common.model.variable.TlMqttPubCompVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttPubCompEncoder extends AbstractTlMqttEncoder<TlMqttPubCompReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubCompReq res, ByteBuf out, MqttMessageType mqttMessageType) {

        TlMqttSession session = (TlMqttSession) ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        MqttVersion mqttVersion = session.getMqttVersion();
        TlMqttPubCompVariableHead variableHead = res.getVariableHead();

        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        writeVariableByteInteger(out, res.getFixedHead().getLength());
        out.writeShort(variableHead.getMessageId().intValue());
        if(mqttVersion == MqttVersion.MQTT_5){

            //最少3个字节 表示2个字节的长度 1个字节的原因码
            String reasonString = variableHead.getReasonString();
            List<UserProperty> userProperties = variableHead.getUserPropertyList();
            byte reasonCode = variableHead.getReasonCode();
            out.writeByte(reasonCode);
            //写入属性的长度
            writeVariableByteInteger(out, variableHead.getPropertiesLength());
            if(reasonString != null){
                out.writeByte(PropertiesCode.REASON_STRING.getCode());
                out.writeShort(reasonString.getBytes(StandardCharsets.UTF_8).length);
                out.writeBytes(reasonString.getBytes(StandardCharsets.UTF_8));
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                for (UserProperty userProperty : userProperties) {
                    out.writeByte(PropertiesCode.USER_PROPERTY.getCode());
                    byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                    out.writeShort(keyBytes.length);
                    out.writeBytes(keyBytes);
                    byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                    out.writeShort(valueBytes.length);
                    out.writeBytes(valueBytes);
                }
            }
        }
    }
}
