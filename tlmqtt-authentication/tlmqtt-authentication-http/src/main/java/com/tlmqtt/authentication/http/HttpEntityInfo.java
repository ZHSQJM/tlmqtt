package com.tlmqtt.authentication.http;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

/**
 * http的认证对象
 * @author  hszhou
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class HttpEntityInfo extends TlAuthenticationSubject {

    public HttpEntityInfo(Long id) {
        setId(id);
        setAuthenticationType(AuthenticationType.HTTP);
    }

    public HttpEntityInfo( ) {

        setAuthenticationType(AuthenticationType.HTTP);
    }

    /**请求的地址*/
    private  String url;
    /**方法类型 post或者get*/
    private  String method;
    /**请求头*/
    private  HashMap<String, String> headers;
    /**用户的参数 例如 参数是uname 与pwd 那么这个params的参数就是("username","uname") ("password","pwd")*/
    private  HashMap<String,String> params;




}
