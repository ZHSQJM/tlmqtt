package com.tlmqtt.auth.http;

import lombok.Data;

import java.util.HashMap;

/**
 * @Author: hszhou
 * @Date: 2025/5/10 18:56
 * @Description: http的认证对象
 */
@Data
public class HttpEntityInfo {

    /**请求的地址*/
    private String url;
    /**方法类型 post或者get*/
    private String method;
    /**请求头*/
    private HashMap<String, String> headers;
    /**用户的参数 例如 参数是uname 与pwd 那么这个params的参数就是("username","uname") ("password","pwd")*/
    private HashMap<String,String> params;

}
