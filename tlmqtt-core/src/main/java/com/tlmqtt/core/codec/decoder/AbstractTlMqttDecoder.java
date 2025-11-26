package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/**
 * @author hszhou
 */
public abstract class  AbstractTlMqttDecoder {


    private static final Integer MAXIMUM_PACKET_SIZE = 65535;
    public AbstractTlMessage decode(ByteBuf buf,int type, int remainingLength, ChannelHandlerContext ctx,   MqttMessageType messageTypeEnum) {
        Object o = ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();

        if(messageTypeEnum==MqttMessageType.CONNECT){
            return build(buf,type, remainingLength,null);
        }else if(null == o){

            //todo 如果不是连接的话 其他报文里面没有这个session的话
        }

        TlMqttSession session = (TlMqttSession)o ;
        assert session != null;
        if(session.getMqttVersion() == MqttVersion.MQTT_5){
            if(buf.readableBytes() > MAXIMUM_PACKET_SIZE){
               throw new TlProtocolErrorException(MqttErrorCode.PACKET_TOO_LARGE,MqttMessageType.DISCONNECT);
            }
        }
        return build(buf,type, remainingLength,session);

    }
    /**
     * 解析完成的数据
     * @author hszhou
     * 2025-05-19 09:12:29
     * @param buf 数据
     * @param type 类型
     * @param remainingLength 剩余长度
     * @param session 会话
     * @return AbstractTlMessage
     **/
    public abstract AbstractTlMessage build(ByteBuf buf,int type, int remainingLength, TlMqttSession session);


    /**
     * 解析剩余长度
     * @author hszhou
     * 2025-05-19 09:12:29
     * @param in 数据
     * @return int
     **/
    public int decodeRemainingLength(ByteBuf in) {
        int multiplier = 1;
        int value = 0;
        byte encodedByte;
        do {
            encodedByte = in.readByte();
            value += (encodedByte & 127) * multiplier;
            multiplier *= 128;
        } while ((encodedByte & 128) != 0);
        return value;
    }
}
