package com.tlmqtt.bootstrap;

import com.tlmqtt.common.Constant;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class ShutDownGracefully {

    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ExecutorService executorService;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public ShutDownGracefully(NioEventLoopGroup boss, NioEventLoopGroup worker, ExecutorService executor) {
        this.bossGroup = boss;
        this.workerGroup = worker;
        this.executorService = executor;
    }

    public void registerShutdownHook(Channel serverChannel) {
        allChannels.add(serverChannel);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "tl-mqtt-shutdown-thread"));
    }

    public void stop() {
        if (!isShuttingDown.compareAndSet(false, true)){
            return;
        }

        log.debug("Stopping TL-MQTT server gracefully...");
        try {
            // 1. 关闭所有 Server Channels
            allChannels.close().awaitUninterruptibly(5, TimeUnit.SECONDS);

            // 2. 停掉 Netty 线程池
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            // 3. 停掉业务线程池
            executorService.shutdown();
            if (!executorService.awaitTermination(Constant.TIMEOUT, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            log.debug("TL-MQTT server stopped.");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
}
