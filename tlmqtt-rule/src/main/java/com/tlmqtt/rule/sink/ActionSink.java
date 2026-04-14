package com.tlmqtt.rule.sink;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@FunctionalInterface
public interface ActionSink {

    /**
     *
     * @author zhouhs 
     * @param: data
     * @param: params 
     **/
    
    void process(Map<String, Object> data, Map<String, Object> params);
}
