package com.tlmqtt.core.retry;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;


/**
 * @Author: hszhou
 * @Date: 2025/5/7 9:57
 * @Description: 放入hashwheeltimer的任务
 */
@Slf4j
@Data
public class TlRetryTask implements TimerTask {

    /**
     * 消息
     */
    private final TlRetryMessage message;
    /**
     * 客户端通道
     */
    private final Channel channel;
    /**
     * 间隔时间
     */
    private final int duration;

    @Setter
    private  Timeout timeout;

    public TlRetryTask(TlRetryMessage message, Channel channel, int duration){
        this.message = message;
        this.channel =channel;
        this.duration = duration;
    }
    @Override
    public void run(Timeout timeout) throws Exception {
        log.info("Resend messageId 【{}】-【{}】",message.getMessageId(),message.getMessage().getClass());
        channel.writeAndFlush(message.getMessage());
        try {
            Thread.sleep(1000);
        }catch (Exception e){
            e.printStackTrace();
        }
        Timer timer = timeout.timer();
        this.timeout = timer.newTimeout(this, duration, TimeUnit.SECONDS);
    }

    public void cancel(){
        if(this.timeout != null){
            this.timeout.cancel();
        }
    }
}
