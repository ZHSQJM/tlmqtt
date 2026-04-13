package com.tlmqtt.rule;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface ActionExecutor {

    /**
     * //对应的是mysql 。kafka，http
     * @author zhouhs
     * @return: java.lang.String
     **/

    String getType();



    /**
     * 
     * @author zhouhs 
     * @param: data
     * @param: config 
     **/
    
    void execute(Map<String,Object> data,Map<String,Object> config);
}
