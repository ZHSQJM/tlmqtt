package com.tlmqtt.auth;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2025/5/10 10:36
 * @Description: 抽象的认证处理器 后续如果需要添加新的认证方式 那么只需要继承该类 并实现对应的方法即可
 */
@Data
@Slf4j
public abstract class AbstractTlAuthentication {


    /**
     * 下一个认证处理器
     */
    protected AbstractTlAuthentication nextAuthentication;


    /**
     * 执行认证方法
     * @author hszhou
     * @datetime: 2025-05-12 15:15:29
     * @param username 用户名
     * @param password 密码
     * @return boolean 是否认证成功
     * 1. 是否开了认证器，如果开启了 那么就执行当前的认证方法 如果认证通过 直接返回 如果认证不通过 那么就获取下一个认证器进行认证
     * 2. 如果没开认证器 那么就执行下一个认证器
     * 3. 如果都认证失败 那么就返回false
     **/
    public boolean execute(String username, String password){
        boolean authenticate = false;

        if(enabled()){
            authenticate = authenticate(username, password);
            if(authenticate){
                return authenticate;
            }
        }
        if(!authenticate && nextAuthentication!=null){
            return nextAuthentication.execute(username,password);
        }
        return false;
    }

    /**
     * 认证接口
     * @author hszhou
     * @datetime: 2025-05-10 10:51:55
     * @param username 用户名
     * @param password 密码
     * @return boolean 是否成功
     **/
    abstract public boolean authenticate(String username, String password);

    /**
     * 是否启用了该认证器
     * @author hszhou
     * @datetime: 2025-05-12 10:11:26
     * @return boolean 是否启用
     **/
    abstract public boolean enabled();


    /**
     * 添加新的认证主体
     * @author hszhou
     * @datetime: 2025-05-15 17:53:17
     * @param object 添加的认证实体
     **/
    abstract public void add(Object object);




}
