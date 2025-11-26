package com.tlmqtt.core.disruptor;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * @author zhouhs
 */
@Data
public class TlMqttEvent {
    private ChannelHandlerContext ctx;
    private AbstractTlMessage message;
    private TlMqttSession session;
}