package com.tlmqtt.common.authentication;


import java.util.List;
import java.util.Map;

/**
 * @author zhouhs
 **/
public abstract class AbstractAuthenticationService {





    /**
     * 初始化认证对象
     * @return Map<AuthenticationType, List<TlAuthenticationSubject>
     */
    public abstract Map<AuthenticationType,List<TlAuthenticationSubject>> init();


}
