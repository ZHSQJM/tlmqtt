package com.tlmqtt.core.service.impl;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.core.service.PublishInterceptor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志记录PublishInterceptor实现示例
 */
@Slf4j
public class LoggingPublishInterceptor<T extends AbstractTlMessage> implements PublishInterceptor<T> {
    
    @Override
    public int order() {
        return 0; // 最低优先级
    }
    
    @Override
    public T intercept(ChannelHandlerContext ctx, T req, TlMqttSession session) {
        String clientId = session != null ? session.getClientId() : "unknown";
        log.info("Publish message intercepted - ClientId: {}", clientId);
        // 不修改消息，直接返回原始消息
        return req;
    }
}