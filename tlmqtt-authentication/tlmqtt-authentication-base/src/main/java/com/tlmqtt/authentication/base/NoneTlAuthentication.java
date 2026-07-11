package com.tlmqtt.authentication.base;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;

import java.util.Collections;
import java.util.List;

/**
 * @author watson
 * @created 2026/7/9
 * @Description
 */
public class NoneTlAuthentication extends AbstractTlAuthentication {

    private final boolean authDisabled;

    public NoneTlAuthentication(boolean authEnabled) {
        // 注意语义：auth.enabled=true 表示开启认证 → authDisabled=false
        this.authDisabled = !authEnabled;
    }

    @Override
    public AuthenticationType getSupportType() {
        return null;
    }

    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }

    @Override
    public boolean enabled() {
        return authDisabled;
    }

    @Override
    public void add(TlAuthenticationSubject subject) {

    }

    @Override
    public void remove(TlAuthenticationSubject subject) {

    }

    @Override
    public List<? extends TlAuthenticationSubject> list() {
        return Collections.emptyList();
    }
}
