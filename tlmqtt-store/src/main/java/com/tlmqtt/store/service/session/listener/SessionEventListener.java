package com.tlmqtt.store.service.session.listener;

import reactor.core.publisher.Mono;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface SessionEventListener {

    /**
     * 当会话被彻底清理（CleanSession 或 过期）时触发
     * @param clientId 客户端ID
     * @return 响应式句柄
     */
    Mono<Void> onSessionCleared(String clientId);
}
