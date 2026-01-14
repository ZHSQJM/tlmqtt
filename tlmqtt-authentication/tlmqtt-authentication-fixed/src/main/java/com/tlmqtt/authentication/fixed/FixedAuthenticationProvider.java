package com.tlmqtt.authentication.fixed;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;

import java.util.ArrayList;

/**
 * @author zhouhs
 **/
public class FixedAuthenticationProvider implements TlAuthenticationProvider {
    @Override
    public AbstractTlAuthentication create() {
        return new FixedTlAuthentication(new ArrayList<>());
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public AuthenticationType name() {
        return AuthenticationType.FIXED;
    }

}