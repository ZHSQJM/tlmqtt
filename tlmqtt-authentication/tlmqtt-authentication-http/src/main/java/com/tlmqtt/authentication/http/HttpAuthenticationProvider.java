package com.tlmqtt.authentication.http;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;

/**
 * @author zhouhs
 **/
public class HttpAuthenticationProvider implements TlAuthenticationProvider {
    @Override
    public AbstractTlAuthentication create() {
        return new HttpTlAuthentication();
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public AuthenticationType name() {
        return AuthenticationType.HTTP;
    }

}