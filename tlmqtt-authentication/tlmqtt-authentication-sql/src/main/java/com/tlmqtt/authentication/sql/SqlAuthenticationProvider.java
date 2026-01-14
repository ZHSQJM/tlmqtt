package com.tlmqtt.authentication.sql;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;

import java.util.ArrayList;

/**
 * @author zhouhs
 **/
public class SqlAuthenticationProvider implements TlAuthenticationProvider {
    @Override
    public AbstractTlAuthentication create() {
        return new SqlTlAuthentication();
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public AuthenticationType name() {
        return AuthenticationType.SQL;
    }

}