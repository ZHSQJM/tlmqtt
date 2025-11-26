package com.tlmqtt.auth.fix;
import com.tlmqtt.auth.AbstractTlAuthentication;
import com.tlmqtt.common.model.entity.TlAuthUser;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于固定的用户名密码认证
 *
 * @author  hszhou
 */
@Slf4j
public class FixTlAuthentication extends AbstractTlAuthentication {

    private final List<TlAuthUser> users;

    public void setUsers(List<TlAuthUser> users) {
        this.users.addAll(users);
    }

    /**
     * 添加新的用户
     * @author hszhou
     * : 2025-05-15 17:52:29
     * @param object 添加的用户
     **/
    @Override
    public void add(Object object) {

        if( object instanceof TlAuthUser){
            log.debug("join fix authentication user 【{}】",object);
            this.users.add((TlAuthUser) object);
        }
    }


    public FixTlAuthentication(List<TlAuthUser> users){
        this.users = users;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if(users.isEmpty()){
            return false;
        }
        for (TlAuthUser user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                //log.debug("username = 【{}】,password = 【{}】 pass",username,password);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean enabled() {
        return true;
    }


}
