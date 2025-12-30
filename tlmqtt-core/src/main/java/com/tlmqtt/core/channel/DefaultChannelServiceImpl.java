package com.tlmqtt.core.channel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class DefaultChannelServiceImpl implements TlChannelService{
    /**
     * 使用 Caffeine 优化：
     * 1. expireAfterWrite: 这里的过期更多是作为一种内存兜底，真正的 MQTT 心跳由 Netty 的 IdleStateHandler 处理
     * 2. removalListener: 当连接被移除（过期、手动删除、替换）时，统一处理 Channel 关闭
     */
    private final Cache<String, Channel> clientCache = Caffeine.newBuilder()
        // 兜底：如果24小时没重连或活动，强制清理
        .expireAfterWrite(24, TimeUnit.HOURS)
        .removalListener((String clientId, Channel channel, RemovalCause cause) -> {
            if (channel != null && channel.isActive()) {
                log.info("Caffeine 移除连接 [{}], 原因: {}, 执行关闭", clientId, cause);
                channel.close();
            }
        })
        .build();

    /**
     * 保存映射关系
     */
    @Override
    public void put(String clientId, Channel newChannel) {
        // Caffeine 的 put 操作会触发 removalListener（如果旧值存在且被替换）
        // 这意味着旧连接的 close() 逻辑会被 removalListener 自动处理，保持代码简洁
        clientCache.put(clientId, newChannel);
       // log.info("客户端 [{}] 绑定新通道 [{}], 当前活跃连接数: {}", clientId, newChannel, clientCache.estimatedSize());
    }

    /**
     * 获取通道
     */
    @Override
    public Channel getChannel(String clientId) {
        return clientCache.getIfPresent(clientId);
    }

    /**
     * 移除映射关系
     */
    @Override
    public void remove(String clientId) {
        // 显式移除会触发 removalListener 中的 channel.close()
        clientCache.invalidate(clientId);
    }
}
