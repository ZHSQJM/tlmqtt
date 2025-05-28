package com.tlmqtt.core.codec;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.utils.TlLog;
import com.tlmqtt.core.codec.decoder.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/15 16:16
 * @Description: mtt的解码器
 */
@RequiredArgsConstructor
public class TlMqttMessageCodec extends ByteToMessageDecoder {


    private final TlMqttConnectDecoder connectDecoder;

    private final TlMqttDisConnectDecoder disConnectDecoder;

    private final TlMqttHeartBeatDecoder heartBeatDecoder;

    private final TlMqttPubAckDecoder pubAckDecoder;

    private final TlMqttPubCompDecoder pubCompDecoder;

    private final TlMqttPublishDecoder publishDecoder;

    private final TlMqttPubRecDecoder pubRecDecoder;

    private final TlMqttPubRelDecoder pubRelDecoder;

    private final TlMqttSubscribeDecoder subscribeDecoder;

    private final TlMqttUnSubscribeDecoder unSubscribeDecoder;
    /**读取的最小字节数*/
    public static final Integer MIN_LENGTH = 2;
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) {


        //如果刻度字节小于2个字节 说明还没有读取到正确的包 因为固定头最少为2个字节
        if (in.readableBytes() < MIN_LENGTH) {
            return;
        }

        //标记一下 后面读取的时候 如果没有读取到正确的值的时候 就可以回到原来的位置 重新涂
        in.markReaderIndex();

        //读取第一个字节 包含消息类型和标志位信息
        short type = in.readUnsignedByte();
        int remainingLength = decodeRemainingLength(in);

        //检查是否接收到了完整的数据包
        if (in.readableBytes() < remainingLength) {
            //如果数据不够 回滚读索引到之前标记的位置 等待更多数据到来
            in.resetReaderIndex();
            return;
        }
        //完整的数据已经接收 读取并解析整个Mqtt的数据包
        ByteBuf messageBuf = in.readBytes(remainingLength);
        TlLog.logger("mqtt 16",messageBuf);
        try {
            int messageType = type >> Constant.MESSAGE_BIT;
            MqttMessageType messageTypeEnum = MqttMessageType.valueOf(messageType);
            AbstractTlMessage req = switch (messageTypeEnum) {
                case CONNECT -> connectDecoder.build(messageBuf,type, remainingLength);
                case DISCONNECT -> disConnectDecoder.build(messageBuf,type, remainingLength);
                case PUBLISH -> publishDecoder.build(messageBuf, type, remainingLength);
                case PUBACK -> pubAckDecoder.build(messageBuf,type, remainingLength);
                case PUBREC -> pubRecDecoder.build(messageBuf,type, remainingLength);
                case PUBREL -> pubRelDecoder.build(messageBuf,type, remainingLength);
                case PUBCOMP -> pubCompDecoder.build(messageBuf,type, remainingLength);
                case SUBSCRIBE -> subscribeDecoder.build(messageBuf,type, remainingLength);
                case UNSUBSCRIBE -> unSubscribeDecoder.build(messageBuf,type, remainingLength);
                case PINGREQ -> heartBeatDecoder.build(messageBuf,type, remainingLength);
                default -> throw new IllegalArgumentException("unknown message type: " + messageTypeEnum);
            };
            out.add(req);
        }finally {
            messageBuf.release();
        }
    }

    private int decodeRemainingLength(ByteBuf in) {
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

