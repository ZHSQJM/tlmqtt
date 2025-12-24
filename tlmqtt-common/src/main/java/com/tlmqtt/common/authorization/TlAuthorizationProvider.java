package com.tlmqtt.common.authorization;


/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface TlAuthorizationProvider {

    /**
     * 校验是否有订阅的权限
     * @author hszhou
     * 2025-06-02 14:58:27
     * @param clientId 客户端id
     * @param username 用户名
     * @param ip ip
     * @param topic 订阅的topic
     * @return boolean
     **/
     boolean checkSubscribePermission(String clientId,String username,String ip,String topic);

    /**
     * 校验是否有发布权限
     * @author hszhou
     * 2025-06-02 14:58:27
     * @param clientId 客户端id
     * @param username 用户名
     * @param ip ip
     * @param topic 主题
     * @return boolean
     **/
    boolean checkPublishPermission(String clientId,String username,String ip,String topic);


    /**
     * 权限提供者排序，数字越小越优先
     * @author hszhou
     * 2025-06-02 14:58:27
     * @return int
     **/
    default int order() { return 100; }



    /**
     * 权限提供者名称
     * @author hszhou
     * 2025-06-02 14:58:27
     * @return java.lang.String
     **/
    String name();
}
