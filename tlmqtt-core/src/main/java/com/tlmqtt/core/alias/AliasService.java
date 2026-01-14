package com.tlmqtt.core.alias;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface AliasService {
    /**
     * 保存别名
     * @author zhouhs
     * @param clientId  客户端
     * @param alias 别名
     * @param topic 主题
     **/
    void put(String clientId, Integer alias, String topic);

    /**
     * 根据别名获取具体别名
     * @author zhouhs
     * @param clientId 客户端
     * @param alias  别名
     * @return String
     **/
    
    String get(String clientId,Integer alias);
}
