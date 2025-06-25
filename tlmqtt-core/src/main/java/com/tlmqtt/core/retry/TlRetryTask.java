package com.tlmqtt.core.retry;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author hszhou
 */
@Slf4j
@Data
public class TlRetryTask implements TimerTask {

    /**
     * 消息ID
     */
    private final Long messageId;

    /**
     * 消息
     */
    private final Object message;
    /**
     * 客户端通道
     */
    private final Channel channel;

    private final AtomicInteger retryCount = new AtomicInteger(0);

    private volatile Timeout timeout;

    private volatile boolean cancelled = false;

    private int maxRetry;

    private int duration;


    @Override
    public void run(Timeout timeout) throws Exception {

        if(cancelled  || channel == null || !channel.isActive()){
            return;
        }
        if(retryCount.incrementAndGet()>maxRetry){
            cancel();
            return;
        }
        try {
            // 异步发送避免阻塞定时器线程
            channel.eventLoop().execute(() -> {
                if (!cancelled && channel.isActive()) {
                    channel.writeAndFlush(message).addListener(future -> {
                        if (future.isSuccess()) {
                            reschedule(timeout.timer());
                        }
                    });
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Event loop overloaded, delaying retry: {}", messageId);
            reschedule(timeout.timer());
        }

    }


    /**
     * 重新安排任务
     * @param timer 定时器
     */
    private void reschedule(Timer timer){
        if(!cancelled){
            this.timeout = timer.newTimeout(this,duration, TimeUnit.SECONDS);
        }
    }

    /**
     * 取消任务
     */
    public void cancel(){
        cancelled = true;
        if(timeout !=null && !timeout.isCancelled()){
            timeout.cancel();
        }
    }
}
