package com.tlmqtt.core.task;

import io.netty.util.Timeout;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 延迟任务处理
 **/
public interface TlSchedulerTaskService {
    /**
     * 提交一个延时任务
     * @param key 任务唯一标识（如 clientId, clientId:msgId）
     * @param task 具体的业务逻辑（Mono）
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return Mono<Void>
     */
    Mono<Void> schedule(String key, Mono<?> task, long delay, TimeUnit unit);

    /**
     * 取消一个已存在的任务
     * @param key 任务唯一标识
     * @return Mono<Void>
     */
    Mono<Void> cancel(String key);


    /**
     * 获取所有任务列表
     * @return 任务列表
     */
    Map<String, Timeout> list();

}
