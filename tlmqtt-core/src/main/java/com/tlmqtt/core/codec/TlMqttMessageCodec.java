package com.tlmqtt.core.codec;


import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.core.codec.decoder.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttMessageCodec extends ByteToMessageDecoder {

    /** 建立消息类型索引表 */
    private final AbstractTlMqttDecoder[] decoders = new AbstractTlMqttDecoder[16];

    public static final int MIN_LENGTH = 2;

    /** MQTT协议规定最大剩余长度 256MB (128^4) */
    private static final int MAX_PAYLOAD_SIZE = 268435455;

    public TlMqttMessageCodec(
        TlMqttConnectDecoder connectDecoder, TlMqttDisConnectDecoder disConnectDecoder,
        TlMqttHeartBeatDecoder heartBeatDecoder, TlMqttPubAckDecoder pubAckDecoder,
        TlMqttPubCompDecoder pubCompDecoder, TlMqttPublishDecoder publishDecoder,
        TlMqttPubRecDecoder pubRecDecoder, TlMqttPubRelDecoder pubRelDecoder,
        TlMqttSubscribeDecoder subscribeDecoder, TlMqttUnSubscribeDecoder unSubscribeDecoder) {

        // 将解码器按类型值存入数组，消除 switch-case
        decoders[MqttMessageType.CONNECT.value()] = connectDecoder;
        decoders[MqttMessageType.DISCONNECT.value()] = disConnectDecoder;
        decoders[MqttMessageType.PUBLISH.value()] = publishDecoder;
        decoders[MqttMessageType.PUBACK.value()] = pubAckDecoder;
        decoders[MqttMessageType.PUBREC.value()] = pubRecDecoder;
        decoders[MqttMessageType.PUBREL.value()] = pubRelDecoder;
        decoders[MqttMessageType.PUBCOMP.value()] = pubCompDecoder;
        decoders[MqttMessageType.SUBSCRIBE.value()] = subscribeDecoder;
        decoders[MqttMessageType.UNSUBSCRIBE.value()] = unSubscribeDecoder;
        decoders[MqttMessageType.PINGREQ.value()] = heartBeatDecoder;
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) {

        if (in.readableBytes() < MIN_LENGTH) {
            return;
        }

        in.markReaderIndex();

        // 1. 读取固定头第一个字节
        short fixedHeader = in.readUnsignedByte();
        int messageType = fixedHeader >> Constant.MESSAGE_BIT;

        // 2. 解码剩余长度 (变长编码)
        int remainingLength = decodeRemainingLength(in);

        // 3. 数据不全或长度非法
        if (remainingLength == -1) {
            in.resetReaderIndex();
            return;
        }

        // 4. 安全检查：防止恶意超大包导致内存溢出
        if (remainingLength > MAX_PAYLOAD_SIZE) {
            log.error("MQTT packet remaining length too large: {}", remainingLength);
            ctx.close(); // 协议违规，直接断开连接
            return;
        }

        // 5. 检查载荷是否完整到达
        if (in.readableBytes() < remainingLength) {
            in.resetReaderIndex();
            return;
        }

        // 6. 获取对应的解码器
        AbstractTlMqttDecoder decoder = decoders[messageType];
        if (decoder == null) {
            log.error("No decoder found for message type: {}", messageType);
            in.skipBytes(remainingLength);
            return;
        }

        // 7. 使用 slice 零拷贝视图读取载荷
        ByteBuf payload = in.readSlice(remainingLength);
        // 此处不需要显式调用 retain() 和 release()，
        // 因为 slice 依赖原 ByteBuf，原 ByteBuf 会在父类逻辑中统一管理。
        // readSlice 会增加读索引，直接操作原内存段。

        try {
            MqttMessageType typeEnum = MqttMessageType.valueOf(messageType);
            AbstractTlMessage req = decoder.decode(payload, fixedHeader, remainingLength, ctx, typeEnum);
            if (req != null) {
                out.add(req);
            }
        } catch (Exception e) {
            log.error("Decode error for message type {}: ", messageType, e);
            // 异常时跳过该包数据
            // 注意：readSlice 已经移动了 index，这里无需手动 skip
            throw e;
        }
    }

    /**
     * 优化点：使用位运算加速变长字节解码
     */
    private int decodeRemainingLength(ByteBuf in) {
        int multiplier = 1;
        int value = 0;
        int bytesRead = 0;
        byte encodedByte;

        do {
            if (in.readableBytes() < 1) {
                return -1;
            }
            encodedByte = in.readByte();
            value += (encodedByte & 0x7F) * multiplier;
            if (multiplier > 128 * 128 * 128) { // 超过 4 字节上限
                return -1;
            }
            multiplier <<= 7; // 等价于 multiplier *= 128，但效率更高
            bytesRead++;
        } while ((encodedByte & 0x80) != 0);

        return value;
    }
}

