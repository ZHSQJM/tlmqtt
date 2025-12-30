package com.tlmqtt.core.service;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Publish消息拦截器接口
 *
 * @version 0.1.0
 * @author zhouhs
 * @since 0.1.0
 **/
public interface PublishInterceptor<TlMqttPublishReq> {
    
    /**
     * 拦截器顺序
     * @return 顺序
     */
    default int order() {
        return 0;
    }

    /**
     * 拦截处理
     * @param ctx ChannelHandlerContext
     * @param req 待处理消息
     * @param session 会话
     * @return 处理后的消息，如果返回null则表示终止处理
     */
    TlMqttPublishReq intercept(ChannelHandlerContext ctx, TlMqttPublishReq req, TlMqttSession session);
}