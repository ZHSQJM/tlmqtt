package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
public class TlMqttPubRelEncoder extends AbstractTlMqttEncoder<TlMqttPubRelReq> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttPubRelReq res, ByteBuf out, MqttMessageType mqttMessageType)  {

        TlMqttSession session = (TlMqttSession) ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();

        TlMqttPubRelVariableHead variableHead = res.getVariableHead();

        //回复订阅
        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        if(session.getMqttVersion() == MqttVersion.MQTT_3_1_1){
            //剩余长度
            out.writeByte(Short.BYTES);
            out.writeShort(variableHead.getMessageId().intValue());
        } else if (session.getMqttVersion()  == MqttVersion.MQTT_5) {

            //最少3个字节 表示2个字节的长度 1个字节的原因码
            int remainingLength = 3;
            int propertiesLength = 0;
            String reasonString = variableHead.getReasonString();
            List<UserProperty> userProperties = variableHead.getUserPropertyList();
            byte reasonCode = variableHead.getReasonCode();
            if(reasonString != null){
                propertiesLength = propertiesLength+1+2+reasonString.getBytes(StandardCharsets.UTF_8).length;
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                for (UserProperty userProperty : userProperties) {
                    byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                    byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                    // 1字节标识 + 2字节key长度 + key + 2字节value长度 + value
                    propertiesLength += 1 + 2 + keyBytes.length + 2 + valueBytes.length;
                }
            }
            if (propertiesLength > 0) {
                remainingLength +=  calculateVariableByteIntegerLength(propertiesLength)+ propertiesLength;
            }else{
                //如果属性长度为0 也要写入
                remainingLength+=1;
            }

            writeVariableByteInteger(out, remainingLength);

            out.writeShort(variableHead.getMessageId().intValue());
            out.writeByte(reasonCode);

            //写入属性的长度
            writeVariableByteInteger(out, propertiesLength);
            if(reasonString != null){
                out.writeByte(0x1F);
                out.writeShort(reasonString.getBytes(StandardCharsets.UTF_8).length);
                out.writeBytes(reasonString.getBytes(StandardCharsets.UTF_8));
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                for (UserProperty userProperty : userProperties) {
                    out.writeByte(0x26);
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
