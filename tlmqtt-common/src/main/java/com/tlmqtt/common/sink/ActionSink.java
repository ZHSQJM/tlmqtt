package com.tlmqtt.common.sink;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@FunctionalInterface
public interface ActionSink {

    /**
     * 收到消息的处理
     * @author zhouhs 
     * @param: data
     * @param: params 
     **/
    
    void process();
}
