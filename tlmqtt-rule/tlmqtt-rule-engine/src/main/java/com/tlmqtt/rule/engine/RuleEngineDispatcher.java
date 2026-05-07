package com.tlmqtt.rule.engine;

import com.tlmqtt.common.rule.BaseSourceRule;
import com.tlmqtt.common.rule.EventContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 规则引擎分发 专业的事情交给专业的规则去做
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class RuleEngineDispatcher {


    private final List<BaseSourceRule> SOURCE_RULES;


    public RuleEngineDispatcher(){
        this.SOURCE_RULES = new ArrayList<>();
        ServiceLoader<BaseSourceRule> sourceRules = ServiceLoader.load(BaseSourceRule.class);
        sourceRules.forEach(e->{
            log.info("加载数据源{}",e.getName());
            SOURCE_RULES.add(e);
        });
    }


    public void dispatch(EventContext eventContext) {
        for (BaseSourceRule rule : SOURCE_RULES) {
            if (rule.shouldProcess(eventContext.getDataSource())) {
                rule.process(eventContext);
                break;
            }
        }

    }
}
