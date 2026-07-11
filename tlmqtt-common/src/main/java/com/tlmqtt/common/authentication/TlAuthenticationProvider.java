package com.tlmqtt.common.authentication;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface TlAuthenticationProvider {
    /**
     创建认证器
     * @return: com.tlmqtt.common.authentication.AbstractTlAuthentication
     **/
    AbstractTlAuthentication create();
    /**
     * 定义该认证器的顺序，越小越靠前
     * @author zhouhs
     * @return: int
     **/
    default int order() { return 100; }
    /**
     * 认证器的名称
     * @author zhouhs
     * @return: java.lang.String
     **/
    AuthenticationType name();
}
