package com.tlmqtt.rule;

import com.tlmqtt.common.enums.MqttDataSource;
import com.tlmqtt.common.rule.BaseSourceRule;
import com.tlmqtt.common.rule.EventContext;
import com.tlmqtt.common.rule.RuleDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class DisConnectLogSourceRule implements BaseSourceRule {


    private final List<RuleDefinition> disConnectRuleDefinition = new ArrayList<>();
    @Override
    public boolean shouldProcess(MqttDataSource dataSource) {

       return dataSource == getName();
    }

    @Override
    public MqttDataSource getName() {
        return MqttDataSource.CLIENT_CONNECTED;
    }

    @Override
    public void process(EventContext ex) {

    }

    @Override
    public void addRuleDefinition(RuleDefinition ruleDefinition) {

    }
}
