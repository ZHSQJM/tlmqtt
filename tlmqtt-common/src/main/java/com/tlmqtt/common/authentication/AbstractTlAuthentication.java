package com.tlmqtt.common.authentication;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 抽象的认证处理器 后续如果需要添加新的认证方式 那么只需要继承该类 并实现对应的方法即可
 *
 * @author  hszhou
 */
@Data
@Slf4j
public abstract class AbstractTlAuthentication {



    private AbstractTlAuthentication next;


    public void setNext(AbstractTlAuthentication next){
        this.next = next;
    }


    public boolean execute(String username,String password){
        if(enabled()){
            if(authenticate(username,password)){
                return true;
            }
        }

        if(next != null){
            return next.execute(username,password);
        }

        return false;
    }

    public void init(List<? extends  TlAuthenticationSubject> subjects){
        if(subjects != null){
            subjects.forEach(this::add);
        }
    }
    /**
     * 获取支持的认证类型 也就是哪种认证方式
     * @return AuthenticationType
     */
    public abstract AuthenticationType getSupportType();
    /**
     * 认证的逻辑
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    public abstract boolean authenticate(String username,String password);

    /**
     * 是否开启这种认证方式
     * @return true or false
     */
    public abstract boolean enabled();

    /**
     * 添加认证的主体
     * @param subject 认证的主体
     */
    public abstract void add(TlAuthenticationSubject subject);

    /**
     * 移除认证的主体
     * @param subject 认证的主体
     */
    public abstract void remove(TlAuthenticationSubject subject);

    /**
     * 展示认证的主体
     * @return 认证的主体列表
     */
    public abstract List<? extends TlAuthenticationSubject> list();



}
