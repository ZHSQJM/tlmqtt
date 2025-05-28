package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttHeartBeat;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: hszhou
 * @Date: 2024/11/30 15:35
 * @Description: puback的编码
 */
@ChannelHandler.Sharable
public class TlMqttHeaderBeatEncoder extends MessageToByteEncoder<TlMqttHeartBeat> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttHeartBeat res, ByteBuf out) throws Exception {
        TlMqttFixedHead fixedHead = res.getFixedHead();
        int messageType = fixedHead.getMessageType().value() << 4;
        out.writeByte(messageType);
        out.writeByte(0);

    }

}
