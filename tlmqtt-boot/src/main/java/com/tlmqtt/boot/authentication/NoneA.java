package com.tlmqtt.boot.authentication;

import com.tlmqtt.auth.AbstractTlAuthentication;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2025/5/21 8:59
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
public class NoneA extends AbstractTlAuthentication {
    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void add(Object object) {

    }
}
