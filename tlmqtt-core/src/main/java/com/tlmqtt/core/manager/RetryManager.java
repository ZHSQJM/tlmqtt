package com.tlmqtt.core.manager;

import com.tlmqtt.core.retry.TlRetryTask;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author hszhou
 */
@Setter
@Slf4j
public class RetryManager extends HashedWheelTimer {


    private int delay;

    private int maxRetry;

    private static final int MAP_SEGMENTS = 32;
    private final ConcurrentMap<Long, TlRetryTask>[] pubSegments;
    private final ConcurrentMap<Long, TlRetryTask>[] pubrelSegments;


    /**
     * 构造函数
     * @param delay  延迟时间
     * @param maxRetry 最大重试次数
     **/
    public RetryManager(int  delay,int maxRetry) {
        this.delay = delay;
        this.maxRetry = maxRetry;
        // 初始化分段Map
        pubSegments = new ConcurrentHashMap[MAP_SEGMENTS];
        pubrelSegments = new ConcurrentHashMap[MAP_SEGMENTS];
        for (int i = 0; i < MAP_SEGMENTS; i++) {
            pubSegments[i] = new ConcurrentHashMap<>();
            pubrelSegments[i] = new ConcurrentHashMap<>();
        }
    }

    /**
     * 获取消息应该存放到哪个map里面
     * @param messageId 消息id
     * @return int
     **/
    private int segmentIndex(Long messageId) {
        return (int) (messageId % MAP_SEGMENTS);
    }


    /**
     * 定时任务发送publish消息
     * @param messageId 消息id
     * @param task  任务
     **/
    public void schedulePublishRetry(Long messageId,TlRetryTask task){
            int segmentIndex = segmentIndex(messageId);
            TlRetryTask retryTask = pubSegments[segmentIndex].putIfAbsent(messageId, task);
            // 如果retryTask返回的是null 则说明messageID的key不在map中 正常返回
            // 如果返回的不是null 怎么说明放入失败了 已经有一个任务了 那么就将这个任务取消 重新放入新的任务
            if(retryTask == null){
                startRetryTask(task);
            }else{
                retryTask.cancel();
                pubSegments[segmentIndex].put(messageId, task);
                startRetryTask(task);
            }
    }


    /**
     * 定时任务发送pubrel消息
     * @param messageId 消息id
     * @param task  任务
     **/
    public void schedulePubrelRetry(Long messageId,TlRetryTask task){

            int segmentIndex = segmentIndex(messageId);
            TlRetryTask retryTask = pubrelSegments[segmentIndex].putIfAbsent(messageId, task);
            if(retryTask == null){
                startRetryTask(task);
            }else{
                retryTask.cancel();
                pubrelSegments[segmentIndex].put(messageId, task);
                startRetryTask(task);
            }
    }

    /**
     * 启动定时任务
     * @param retryTask  任务
     **/
    public void startRetryTask(TlRetryTask retryTask){
        retryTask.setDuration(delay);
        retryTask.setMaxRetry(maxRetry);
        Timeout timeout = this.newTimeout(retryTask, delay, TimeUnit.SECONDS);
        retryTask.setTimeout(timeout);
    }

    /**
     * 取消定时任务
     * @param messageId 消息id
     **/
    public void cancelPublishRetry(Long messageId){
            int segmentIndex = segmentIndex(messageId);
            TlRetryTask retryTask = pubSegments[segmentIndex].remove(messageId);
            if(retryTask!=null){
                retryTask.cancel();
            }
    }

    /**
     * 取消定时任务
     * @param messageId 消息id
     **/
    public void cancelPubrelRetry(Long messageId){
            int segmentIndex = segmentIndex(messageId);
            TlRetryTask retryTask = pubrelSegments[segmentIndex].remove(messageId);
            if(retryTask!=null){
                retryTask.cancel();
            }
    }
}