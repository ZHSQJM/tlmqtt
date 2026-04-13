package com.tlmqtt.rule;

import com.googlecode.aviator.AviatorEvaluator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class RuleEngineProcessor {

    /**所有的规则列表*/
    private List<RuleDefinition> rules = new CopyOnWriteArrayList<>();

    public void processMessage(String topic, Map<String, Object> payload) {
        for (RuleDefinition rule : rules) {
            // 1. Topic 匹配 (使用简单的通配符检查)
            if (!topicMatches(rule.getTopicFilter(), topic)) continue;

            // 2. 条件匹配 (执行表达式)
            // payload 里的数据会自动映射为表达式里的变量
            Boolean isMatch = (Boolean) AviatorEvaluator.execute(rule.getCondition(), payload);

            if (isMatch) {
                // 3. 执行动作
                executeActions(rule.getActions(), payload);
            }
        }
    }
}
