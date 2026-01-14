package com.tlmqtt.authentication.fixed;
import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import com.tlmqtt.common.model.entity.TlAuthUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 基于固定的用户名密码认证
 * @author  hszhou
 */
@Slf4j
public class FixedTlAuthentication extends AbstractTlAuthentication {

    private final List<TlAuthUser> users;


    /**
     * 添加新的用户
     * @author hszhou
     * 2025-05-15 17:52:29
     * @param object 添加的用户
     **/
    @Override
    public void add(TlAuthenticationSubject object) {
        if( object instanceof TlAuthUser){
            TlAuthUser user = (TlAuthUser) object;
            log.debug("【TLMQTT】 Add Fixed User 【{}】",user);
            this.users.add(user);
        }
    }

    @Override
    public void remove(TlAuthenticationSubject object) {
        if( object instanceof TlAuthUser){
            TlAuthUser user = (TlAuthUser) object;
            log.debug("【TLMQTT】 remove Fixed User 【{}】",user);
            users.remove( user);
        }
    }

    @Override
    public List<TlAuthUser> list() {
        return users;
    }

    public FixedTlAuthentication(List<TlAuthUser> users){
        this.users = users;
    }

    @Override
    public AuthenticationType getSupportType() {
        return AuthenticationType.FIXED;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if(users.isEmpty()){
            log.debug("【TLMQTT】 FixedTlAuthentication users is empty");
            return false;
        }
        for (TlAuthUser user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                log.debug("【TLMQTT】 FixedTlAuthentication  authentication success! username = 【{}】",user.getUsername());
                return true;
            }
        }
        log.debug("【TLMQTT】 FixedTlAuthentication  authentication fail!");
        return false;
    }

    @Override
    public boolean enabled() {
        return true;
    }


}
