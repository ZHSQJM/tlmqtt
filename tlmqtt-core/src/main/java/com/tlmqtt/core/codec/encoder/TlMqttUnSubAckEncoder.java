package com.tlmqtt.core.codec.encoder;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;
import com.tlmqtt.common.model.variable.TlMqttUnSubAckVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttUnSubAckEncoder extends AbstractTlMqttEncoder<TlMqttUnSubAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttUnSubAck req, ByteBuf out, MqttMessageType mqttMessageType) {
        TlMqttUnSubAckVariableHead variableHead = req.getVariableHead();
        TlMqttFixedHead fixedHead = req.getFixedHead();
        int type = fixedHead.getMessageType().value() << 4;
        //消息类型
        out.writeByte(type);
        //剩余长度
        out.writeByte(Short.BYTES);
        out.writeShort(variableHead.getMessageId());

        log.debug("Send 【UNSUBACK】 message to client:【{}】", variableHead.getMessageId());

    }
}