package com.tlmqtt.bootstrap;

import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import lombok.Getter;

/**
 * @author zhouhs
 **/
@Getter
public class DynamicTrafficShaper {

    private final GlobalTrafficShapingHandler handler;

    public DynamicTrafficShaper(MultiThreadIoEventLoopGroup workerGroup, long writeLimit, long readLimit,long checkInterval,long maxTime) {
        // Netty 官方建议：GlobalTrafficShapingHandler 应该是全局单例的
        this.handler = new GlobalTrafficShapingHandler(workerGroup, writeLimit, readLimit, checkInterval, maxTime);
    }

    /**
     * 优雅点：支持运行时动态修改限速
     */
    public void updateLimits(long newWriteLimit, long newReadLimit) {
        handler.configure(newWriteLimit, newReadLimit);
    }
}
