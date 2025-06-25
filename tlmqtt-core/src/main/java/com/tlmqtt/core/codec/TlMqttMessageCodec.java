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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author hszhou
 */
@RequiredArgsConstructor
@Slf4j
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

    /**MQTT 剩余长度的最大字节数*/
    private static final int MAX_REMAINING_LENGTH_BYTES = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) {

        //1. 检查基本长度：可读字节数小于2(固定头最小长度)，等待更多数据
        if (in.readableBytes() < MIN_LENGTH) {
            return;
        }
        // 2. 标记当前读指针位置，以便后续数据不足时回退
        in.markReaderIndex();
        // 3. 读取第1字节：包含消息类型(高4位)和标志位(低4位)
        short type = in.readUnsignedByte();
        // 4. 解码剩余长度 (变长编码)
        int remainingLength = decodeRemainingLength(in);
        // 5. 检查载荷数据是否完整到达 (剩余长度指的就是载荷长度)
        if (remainingLength==-1 ||in.readableBytes() < remainingLength) {
            //如果数据不够 回滚读索引到之前标记的位置 等待更多数据到来
            in.resetReaderIndex();
            return;
        }
        // 6. 数据完整：读取载荷部分到临时ByteBuf // 使用slice避免复制
        ByteBuf messageBuf = in.readSlice(remainingLength).retain();
        TlLog.logger("mqtt 16 radix",messageBuf);
        try {
            // 7. 提取消息类型 (右移4位取高4位)
            int messageType = type >> Constant.MESSAGE_BIT;
            MqttMessageType messageTypeEnum = MqttMessageType.valueOf(messageType);
            // 8. 根据消息类型，分派给对应的具体解码器构建请求对象
            AbstractTlMessage req;
            switch (messageTypeEnum) {
                case CONNECT:
                    req = connectDecoder.build(messageBuf,type, remainingLength);
                    break;
                case DISCONNECT:
                    req = disConnectDecoder.build(messageBuf,type, remainingLength);
                    break;
                case PUBLISH:
                    req = publishDecoder.build(messageBuf, type, remainingLength);
                    break;
                case PUBACK:
                    req = pubAckDecoder.build(messageBuf,type, remainingLength);
                    break;
                case PUBREC:
                    req = pubRecDecoder.build(messageBuf,type, remainingLength);
                    break;
                case PUBREL:
                    req = pubRelDecoder.build(messageBuf,type, remainingLength);
                    break;
                case PUBCOMP:
                    req = pubCompDecoder.build(messageBuf,type, remainingLength);
                    break;
                case SUBSCRIBE:
                    req = subscribeDecoder.build(messageBuf,type, remainingLength);
                    break;
                case UNSUBSCRIBE:
                    req = unSubscribeDecoder.build(messageBuf,type, remainingLength);
                    break;
                case PINGREQ:
                    req = heartBeatDecoder.build(messageBuf,type, remainingLength);
                    break;
                default:
                    throw new IllegalArgumentException("unknown message type: " + messageTypeEnum);
            }
            // 9. 将解析好的消息对象加入输出列表，传递给后续Handler
            out.add(req);
        }finally {
            // 10. 确保临时ByteBuf资源释放
            messageBuf.release();
        }
    }

    private int decodeRemainingLength(ByteBuf in) {
        int multiplier = 1;
        int value = 0;
        byte encodedByte;
        int bytesRead = 0;
        do {
            // 检查是否有足够数据可读
            if (in.readableBytes() < 1) {
                return -1;
            }
            encodedByte = in.readByte();
            value += (encodedByte & 127) * multiplier;
            multiplier *= 128;
            bytesRead++;
            if (bytesRead > MAX_REMAINING_LENGTH_BYTES) {
                return -1;
            }
        } while ((encodedByte & 128) != 0);
        return value;
    }
}

