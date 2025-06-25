package com.tlmqtt.core.server;

import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hszhou

 */
@Slf4j
public class ShutDownGracefully {

    /**服务端*/
    private  Channel serverChannel;

    /**boss线程组*/
    private final NioEventLoopGroup bossGroup;

    /**work线程组*/
    private final NioEventLoopGroup workerGroup;

    /**线程池*/
    private final  ExecutorService executorService;

    /**关闭状态标志*/
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**已完全停止*/
    private final AtomicBoolean isStopped = new AtomicBoolean(false);



    /**
     * 构造函数
     * @param serverChannel 服务端通道
     * @param bossGroup boss线程组
     * @param workerGroup work线程组
     * @param executorService 业务线程池
     */
    public ShutDownGracefully(Channel serverChannel,NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup,ExecutorService executorService){
        this.serverChannel =serverChannel;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.executorService = executorService;
    }



    /**
     * 注册关闭钩子
     * @param channel 服务端通道
     */
    public void registerShutdownHook(Channel channel) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.serverChannel =channel;
            log.info("Shutdown hook triggered, stopping server gracefully...");
            stop();
        }, "mqtt-shutdown-hook"));
    }
    /**
     * 停止服务器并释放所有资源
     */
    public synchronized void stop() {
        if (isShuttingDown.getAndSet(true)) {
            log.info("Shutdown already in progress");
            return;
        }

        log.info("Starting graceful shutdown...");

        try {
            // 1. 关闭服务器通道（停止接受新连接）
            if (serverChannel != null) {
                log.info("Closing server channel...");
                serverChannel.close().sync();
                serverChannel = null;
            }

            // 2. 关闭Netty线程组
            shutdownNettyGroups();

            // 3. 关闭业务线程池
            shutdownBusinessExecutor();

            log.info("MQTT server stopped gracefully");
        } catch (InterruptedException e) {
            log.warn("Shutdown interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        } finally {
            isStopped.set(true);
        }
    }
    /**
     * 优雅关闭业务线程池
     */
    private void shutdownBusinessExecutor() {
        log.info("Shutting down business executor...");

        // 1. 停止接受新任务
        executorService.shutdown();

        try {
            // 2. 等待现有任务完成（30秒超时）
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Business executor not terminated, forcing shutdown");

                // 3. 强制取消所有剩余任务
                executorService.shutdownNow();

                // 4. 额外等待
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Business executor did not terminate");
                }
            } else {
                log.info("Business executor terminated gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
    /**
     * 优雅关闭Netty线程组
     */
    private void shutdownNettyGroups() throws InterruptedException {
        log.info("Shutting down worker group...");
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }

        log.info("Shutting down boss group...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS).sync();
        }
    }
}
