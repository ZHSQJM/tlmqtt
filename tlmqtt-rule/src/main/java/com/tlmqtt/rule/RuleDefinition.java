package com.tlmqtt.rule;

import lombok.Data;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
public class RuleDefinition {

    private String ruleId;

    /** a/b/c*/
    private String topicFilter;
    /**temp>30 && status == "online"*/
    private String condition;

    /**[{"type":"mysql","table":"logs"},{"type:kafka",topic:"logs"}]*/
    private List<ActionConfig> actions;
}
