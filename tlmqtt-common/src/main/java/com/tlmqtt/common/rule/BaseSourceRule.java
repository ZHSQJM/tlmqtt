package com.tlmqtt.common.rule;

import com.tlmqtt.common.sink.ActionSink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public abstract class BaseSourceRule {


    /**该规则关联的动作列表*/
    private List<ActionSink> sinks = new ArrayList<>();
    /***
     * 是否处理该数据源
     * @author zhouhs
     * @param: context
     * @return: boolean
     **/

    public abstract boolean shouldProcess(EventContext context);


    public void  process(EventContext context){
        if(shouldProcess(context)){
            sinks.forEach(e->e.process());
        }
    }

    public void addSink(ActionSink sink){
        sinks.add(sink);
    }
}
