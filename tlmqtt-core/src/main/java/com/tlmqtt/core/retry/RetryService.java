package com.tlmqtt.core.retry;

import com.tlmqtt.core.channel.ChannelManager;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 15:49
 * @Description: 重试
 */
@Setter
@Slf4j
public class RetryService  extends HashedWheelTimer {

    private final ChannelManager channelManager;

    private int delay;

    public ConcurrentHashMap<String, TlRetryTask> map = new ConcurrentHashMap<>();

    public RetryService(ChannelManager channelManager, int  delay) {
        this.channelManager = channelManager;
        this.delay = delay;
    }


    public void retry(TlRetryMessage req, String clientId) {

        Channel channel = channelManager.getChannel(clientId);
        if (channel != null && channel.isActive()) {
            TlRetryTask retryTask = new TlRetryTask(req, channel, delay);
            Timeout timeout = this.newTimeout(retryTask, delay, TimeUnit.SECONDS);
            retryTask.setTimeout(timeout);
            map.put(req.getMessageId().toString(), retryTask);
        }

    }

    public void cancel(Long messageId) {
        String msgId = messageId.toString();
        TlRetryTask tlRetryTask = map.get(msgId);
        if (tlRetryTask != null) {
            tlRetryTask.cancel();
            map.remove(msgId);
        }
    }

    public void removeSession(String clientId) {

    }
}