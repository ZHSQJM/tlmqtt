package com.tlmqtt.auth;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * @Author: hszhou
 * @Date: 2025/5/12 10:57
 * @Description: 是否开启认证 如果不开启 那么就无需认证  认证链上的第一个
 */
@RequiredArgsConstructor
@Slf4j
@Setter
public class NoneAuthenticationService extends AbstractTlAuthentication{

    private  boolean enabled;

    public NoneAuthenticationService(Supplier<Boolean> supplier){
        this.enabled =supplier.get();
    }
    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }

    @Override
    public boolean enabled() {
        return !enabled;
    }

    @Override
    public void add(Object object) {

    }


}
