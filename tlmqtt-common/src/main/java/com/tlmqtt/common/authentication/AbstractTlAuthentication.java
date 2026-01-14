package com.tlmqtt.common.authentication;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 抽象的认证处理器 后续如果需要添加新的认证方式 那么只需要继承该类 并实现对应的方法即可
 *
 * @author  hszhou
 */
@Data
@Slf4j
public abstract class AbstractTlAuthentication {


    protected AbstractTlAuthentication next;

    public void setNext(AbstractTlAuthentication next) { this.next = next; }

    public boolean execute(String username, String password) {
        if (enabled()) {
            if (authenticate(username, password)) return true;
        }
        return next != null && next.execute(username, password);
    }

    public abstract AuthenticationType getSupportType();

    public abstract boolean authenticate(String username, String password);
    public abstract boolean enabled();
    public abstract void add(TlAuthenticationSubject subject);
    public abstract void remove(TlAuthenticationSubject subject);
    public abstract List<? extends TlAuthenticationSubject> list();



}
