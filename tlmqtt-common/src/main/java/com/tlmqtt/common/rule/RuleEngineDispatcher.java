package com.tlmqtt.common.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 规则引擎分发 专业的事情交给专业的规则去做
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class RuleEngineDispatcher {

    // 存储所有规则（现在先硬编码）
    private List<BaseSourceRule> rules = new ArrayList<>();

    // 使用线程池，防止规则处理太慢拖垮 Broker
    private static final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void dispatch(EventContext ctx) {
        for (BaseSourceRule rule : rules) {

            workerPool.execute(()->{
                // 1. 判断规则是否关心这个事件
                if (rule.shouldProcess(ctx)) {
                    // 2. 执行逻辑
                    rule.process(ctx);
                }
            });
        }
    }

}
