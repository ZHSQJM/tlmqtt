package com.tlmqtt.authentication.sql;

import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;

import java.util.ArrayList;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class SqlAuthenticationProvider implements TlAuthenticationProvider {
    @Override
    public AbstractTlAuthentication create() {
        return new SqlTlAuthentication(new ArrayList<>());
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public String name() {
        return "SQL";
    }
}