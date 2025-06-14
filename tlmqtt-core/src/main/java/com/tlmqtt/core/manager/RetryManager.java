package com.tlmqtt.core.manager;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.core.retry.TlRetryMessage;
import com.tlmqtt.core.retry.TlRetryTask;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 15:49
 * @Description: 重试
 */
@Setter
@Slf4j
public class RetryManager extends HashedWheelTimer {

    private final ChannelManager channelManager;

    private int delay;

    public ConcurrentHashMap<String, TlRetryTask> map = new ConcurrentHashMap<>();

    public RetryManager(ChannelManager channelManager, int delay) {
        this.channelManager = channelManager;
        this.delay = delay;
    }

    public void retry(TlRetryMessage req, String clientId, MqttMessageType type) {

        Long messageId = req.getMessageId();
        log.debug(" retry messageId:【{}】，类型是【{}】", messageId,type);
        Channel channel = channelManager.getChannel(clientId);
        if (channel != null && channel.isActive()) {
            TlRetryTask retryTask = new TlRetryTask(req, channel, delay);
            Timeout timeout = this.newTimeout(retryTask, delay, TimeUnit.SECONDS);
            retryTask.setTimeout(timeout);
            map.put(messageId.toString(), retryTask);
            //通知
        } else {
            map.remove(messageId.toString());
        }
        log.debug("保存完毕【{}】",type);
    }

    /**
     * 取消发送
     *
     * @param messageId 消息ID
     * @author hszhou
     * @datetime: 2025-06-07 08:56:56
     **/
    public void cancel(Long messageId) {
        String msgId = messageId.toString();
        TlRetryTask tlRetryTask = map.get(msgId);
        log.debug("cancel task 【{}】，【{}】",messageId,tlRetryTask.getMessage().getMessage().getClass());
        tlRetryTask.cancel();
        map.remove(msgId);
    }

}