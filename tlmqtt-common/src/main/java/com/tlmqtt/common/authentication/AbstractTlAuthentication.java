package com.tlmqtt.common.authentication;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 抽象的认证处理器 后续如果需要添加新的认证方式 那么只需要继承该类 并实现对应的方法即可
 *
 * @author  hszhou
 */
@Setter
@Data
@Slf4j
public abstract class AbstractTlAuthentication {


    protected AbstractTlAuthentication next;

    public boolean execute(String username, String password) {
        if (enabled()) {
            if (authenticate(username, password)) {
                return true;
            }
        }
        return next != null && next.execute(username, password);
    }

    /**
     * 获取支持的认证方式
     * @return AuthenticationType
     */
    public abstract AuthenticationType getSupportType();

    /**
     * 认证方法
     * @param username 用户名
     * @param password 密码
     * @return boolean
     */
    public abstract boolean authenticate(String username, String password);

    /**
     * 认证是否启用
     * @return boolean
     */
    public abstract boolean enabled();
    /**
     * 添加认证对象
     * @param subject 认证对象
     */
    public abstract void add(TlAuthenticationSubject subject);
    /**
     * 删除认证对象
     * @param subject 认证对象
     */
    public abstract void remove(TlAuthenticationSubject subject);
    /**
     * 获取认证对象列表
     * @return List<TlAuthenticationSubject>
     */
    public abstract List<? extends TlAuthenticationSubject> list();



}
