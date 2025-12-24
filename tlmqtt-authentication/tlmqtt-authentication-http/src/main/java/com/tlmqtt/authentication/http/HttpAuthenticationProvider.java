package com.tlmqtt.authentication.http;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;

import java.util.ArrayList;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class HttpAuthenticationProvider implements TlAuthenticationProvider {
    @Override
    public AbstractTlAuthentication create() {
        return new HttpTlAuthentication(new ArrayList<>());
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public String name() {
        return "HTTP";
    }
}