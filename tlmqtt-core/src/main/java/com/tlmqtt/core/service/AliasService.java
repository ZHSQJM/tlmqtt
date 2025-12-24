package com.tlmqtt.core.service;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface AliasService {




    /**
     * 
     * @author zhouhs 
     * @param: topic
     * @param: alias 
     **/

    boolean put(String clientId, Integer alias, String topic);


    /**
     * 
     * @author zhouhs 
     * @param: alias 
     * @return: java.lang.String
     **/
    
    String get(String clientId,Integer alias);
}
