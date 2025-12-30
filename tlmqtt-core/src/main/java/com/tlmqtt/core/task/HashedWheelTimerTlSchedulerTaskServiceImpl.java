package com.tlmqtt.core.task;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于hash wheel的延迟任务处理
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class HashedWheelTimerTlSchedulerTaskServiceImpl implements TlSchedulerTaskService {


    private final Timer timer = new HashedWheelTimer();

    private final Map<String, Timeout> tasks = new ConcurrentHashMap<>();


    @Override
    public Mono<Void> schedule(String key, Mono<?> task, long delay, TimeUnit unit) {


        return Mono.fromRunnable(() -> {

            // 如果 expirySeconds 为 0，应该立即清理 (由 Handler 判断或这里判断)
            if (delay <= 0) {
                task.subscribe(
                    v -> log.debug("Scheduled task [{}] executed.", key),
                    e -> log.error("Scheduled task [{}] failed.", key, e)
                );
            }else{

                // 使用 compute 确保原子性：先取消旧的，再创建新的
                tasks.compute(key, (k, oldTimeout) -> {
                    if (oldTimeout != null && !oldTimeout.isExpired()) {
                        oldTimeout.cancel();
                    }
                    // 创建新的定时任务
                    return timer.newTimeout(timeout -> {
                        // 任务触发时的逻辑
                        tasks.remove(key); // 1. 先从 map 移除
                        if (timeout.isCancelled()) {
                            return;
                        }

                        // 2. 执行业务逻辑
                        task.subscribe(
                            unused -> log.debug("任务 [{}] 执行成功", key),
                            error -> log.error("任务 [{}] 执行失败", key, error)
                        );
                    }, delay, unit);

                });
            }


        });

    }

    @Override
    public Mono<Void> cancel(String key) {
        return Mono.defer(() -> {
            log.debug("尝试取消任务 Key: [{}]", key);

            // 使用原子操作移除
            Timeout timeout = tasks.remove(key);

            if (timeout == null) {
                return Mono.empty();
            }

            log.debug("任务 [{}] 存在，当前状态: cancelled={}, expired={}",
                key, timeout.isCancelled(), timeout.isExpired());

            if (!timeout.isCancelled()) {
                timeout.cancel();
                log.info("任务 [{}] 已成功调用 cancel()", key);
            }

            return Mono.empty();
        });
    }

    @Override
    public Map<String, Timeout> list() {

        return tasks;
    }

}
