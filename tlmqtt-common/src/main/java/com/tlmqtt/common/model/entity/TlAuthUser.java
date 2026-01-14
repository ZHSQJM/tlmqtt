package com.tlmqtt.common.model.entity;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
/**
 * 本地文件用户信息
 *
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class TlAuthUser  extends TlAuthenticationSubject {

    /****
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private  String password;



    public TlAuthUser(){
        setAuthenticationType(AuthenticationType.FIXED);
    }

}
