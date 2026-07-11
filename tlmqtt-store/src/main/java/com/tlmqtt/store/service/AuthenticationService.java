package com.tlmqtt.store.service;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;

import java.util.List;
import java.util.Map;

/**
 * @author watson
 * @created 2026/7/7
 * @Description
 */
public interface AuthenticationService {


    /**
     * 初始换加载所以的认证数据
     * @return Map<认证类型, 该类型下的认证主体列表>
     */
    Map<AuthenticationType,List<TlAuthenticationSubject>> init();


    /**
     * 保存一个认证主体
     * @param subject 认证主体
     */
    void save(TlAuthenticationSubject subject);

    /**
     * 删除一个认证主体
     * @param subject
     */
    void delete(TlAuthenticationSubject subject);

    /**
     * 查询某个类型的所有认证主体
     * @param type 认证类型
     * @return
     */
    List<TlAuthenticationSubject> getByType(AuthenticationType type);
}
