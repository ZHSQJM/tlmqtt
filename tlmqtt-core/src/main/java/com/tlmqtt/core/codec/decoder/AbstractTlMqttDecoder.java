package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.TlConfig;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlMalformedPacketException;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
public abstract class  AbstractTlMqttDecoder {



    public AbstractTlMessage decode(ByteBuf buf,int type, int remainingLength, ChannelHandlerContext ctx,   MqttMessageType messageTypeEnum) {
        Object  sessionValue = ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        if(messageTypeEnum==MqttMessageType.CONNECT){
            //todo 在一个网络连接上，客户端只能发送一次CONNECT报文。服务端必须将客户端发送的第二个CONNECT报文当作协议违规处理并断开客户端的连接 [MQTT-3.1.0-2]。有关错误处理的信息请查看4.13节。
            if(sessionValue != null){
                throw new TlProtocolErrorException(messageTypeEnum);
            }
            return build(buf,type, remainingLength,null);
        }else if(null == sessionValue){
            //todo 如果sessionValue的值为空 说明ctx之前没有连接过
            throw new TlMalformedPacketException(messageTypeEnum);
        }
        TlMqttSession session = (TlMqttSession)sessionValue;
        if(session.getMqttVersion() == MqttVersion.MQTT_5){
            if(buf.readableBytes() > TlConfig.getInt( TlConfig.MAXIMUM_PACKET_SIZE)){
               throw new TlProtocolErrorException(MqttMessageType.DISCONNECT);
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

    /**
     * 获取当前时间。返回的是秒
     * @author zhouhs
     * @param: null
     * @return: null
     **/
    public Long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

}
