package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hszhou
 * @Date: 2025/6/6 12:04
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class TlMqttDecoder extends AbstractTlMqttDecoder{
    @Override
    public AbstractTlMessage build(ByteBuf buf, int type, int remainingLength) {
        return null;
    }
}
