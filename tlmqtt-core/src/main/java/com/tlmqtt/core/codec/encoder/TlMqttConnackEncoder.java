package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttConnack;
import com.tlmqtt.common.model.variable.TlMqttConnackVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttConnackEncoder extends MessageToByteEncoder<TlMqttConnack> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttConnack res, ByteBuf out) throws Exception {
        TlMqttFixedHead fixedHead = res.getFixedHead();
        TlMqttConnackVariableHead variableHead = res.getVariableHead();
        int currentSession = variableHead.getCurrentSession();
        int code = variableHead.getCode();
        MqttMessageType messageType = fixedHead.getMessageType();
        //消息类型
        out.writeByte(messageType.value() << 4);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeByte(currentSession);
        out.writeByte(code);
    }

}
