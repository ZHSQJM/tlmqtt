package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;

/**
 * @author hszhou
 */
public class TlMqttDecoder extends AbstractTlMqttDecoder{
    @Override
    public AbstractTlMessage build(ByteBuf buf, int type, int remainingLength) {
        return null;
    }
}
