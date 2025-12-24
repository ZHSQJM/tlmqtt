package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.payload.TlMqttSubAckPayload;
import com.tlmqtt.common.model.response.TlMqttSubAck;
import com.tlmqtt.common.model.variable.TlMqttSubAckVariableHead;
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
public class TlMqttSubAckEncoder extends AbstractTlMqttEncoder<TlMqttSubAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttSubAck res, ByteBuf out, MqttMessageType mqttMessageType) {

        TlMqttSession session = (TlMqttSession) ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        MqttVersion mqttVersion = session.getMqttVersion();
        TlMqttSubAckVariableHead variableHead = res.getVariableHead();
        TlMqttSubAckPayload payload = res.getPayload();
        int[] codes = payload.getCodes();
        //回复订阅
        int type = res.getFixedHead().getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        if(mqttVersion == MqttVersion.MQTT_3_1_1){
            //剩余长度
            out.writeByte(Short.BYTES + codes.length);
            out.writeShort(variableHead.getMessageId());
            for (int code : codes) {
                out.writeByte(code);
            }
        }else if(mqttVersion == MqttVersion.MQTT_5){
            // 剩余长度  2个字节的报文标识符
            int remainingLength = Short.BYTES+ codes.length;
            int propertiesLength = 0;
            String reasonString = variableHead.getReasonString();
            List<UserProperty> userProperties = variableHead.getUserPropertyList();

            if(reasonString != null){
                //一个字节的标识符 +2个字节的长度 + 原因字符串的长度
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
            }
            else{
                //如果属性长度为0 也要写入
                remainingLength+=1;
            }
            writeVariableByteInteger(out, remainingLength);

            //写入报文标识符
            out.writeShort(variableHead.getMessageId());

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
            for (int code : codes) {
                out.writeByte(code);
            }
        }

    }

}