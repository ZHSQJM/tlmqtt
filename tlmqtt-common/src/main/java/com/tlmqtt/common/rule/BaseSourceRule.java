package com.tlmqtt.common.rule;

import com.tlmqtt.common.enums.MqttDataSource;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface BaseSourceRule {



    /**
     * 是否处理
     * @author zhouhs
     * @param: mqttDataSource 数据源类型
     * @return: boolean 是否能处理
     **/

    boolean shouldProcess(MqttDataSource mqttDataSource);



    /**
     * 获取数据源的名称
     * @author zhouhs
     * @return: com.tlmqtt.common.enums.MqttDataSource
     **/

    MqttDataSource getName();


    /**
     * 处理
     * @author zhouhs
     * @param: ex 上下文
     **/

    void process(EventContext ex);


    /**
     * 添加规则
     * @author zhouhs
     * @param: ruleDefinition 自定义规则
     **/

    void addRuleDefinition(RuleDefinition ruleDefinition);
}
