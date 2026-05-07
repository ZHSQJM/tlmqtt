package com.tlmqtt.rule;

import com.tlmqtt.common.enums.MqttDataSource;
import com.tlmqtt.common.rule.BaseSourceRule;
import com.tlmqtt.common.rule.EventContext;
import com.tlmqtt.common.rule.RuleDefinition;
import com.tlmqtt.common.sink.SinkProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class ConnectLogSourceRule implements BaseSourceRule {


    private final List<RuleDefinition> connectRuleDefinition = new ArrayList<>();
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

        for (RuleDefinition rule : connectRuleDefinition) {
            if(rule.getSourceEvents() != MqttDataSource.CLIENT_CONNECTED){
                continue;
            }
            List<SinkProvider> sinks = rule.getSinks();
            String clientId = ex.getClientId();
            log.info("执行连接操作的规则引擎");
        }
    }

    @Override
    public void addRuleDefinition(RuleDefinition ruleDefinition) {

        if(ruleDefinition.getSourceEvents() != MqttDataSource.CLIENT_CONNECTED){
            return;
        }
        this.connectRuleDefinition.add(ruleDefinition);
    }
}
