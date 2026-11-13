package com.tlmqtt.common.interceptor;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志记录 PublishInterceptor 实现示例
 *
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class LoggingPublishInterceptor implements PublishInterceptor {


    @Override
    public TlMqttPublishReq intercept(ChannelHandlerContext ctx, TlMqttPublishReq req, TlMqttSession session) {
        String clientId = session != null ? session.getClientId() : "unknown";
        log.debug("【TLMQTT】Publish message intercepted - ClientId: {}, Topic: {}", clientId,
            req.getVariableHead() != null ? req.getVariableHead().getTopic() : "unknown");
        // 不修改消息，直接返回原始消息
        return req;
    }
}
