package com.tlmqtt.boot.authentication.http;

import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.http.HttpTlAuthentication;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;

import java.util.HashMap;

/**
 * @Author: hszhou
 * @Date: 2025/5/29 15:24
 * @Description: 提供http认证接口示例
 */
public class AuthenticationHttpProvider {


    public static HttpEntityInfo formLogin(){
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        HttpEntityInfo entityInfo = new HttpEntityInfo();
        entityInfo.setUrl("http://127.0.0.1:8097/mqtt/login/test");
        entityInfo.setMethod(HttpPost.METHOD_NAME);
        entityInfo.setHeaders(headers);
        return entityInfo;
    }


    public static HttpEntityInfo postLogin(){
        HashMap<String, String> headers = new HashMap<>(16);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        HttpEntityInfo entityInfo = new HttpEntityInfo();
        entityInfo.setUrl("http://127.0.0.1:8097/mqtt/login");
        entityInfo.setMethod(HttpPost.METHOD_NAME);
        entityInfo.setHeaders(headers);
        HashMap<String, String> params = new HashMap<>(1);
        //指定参数的名称 如果不是username或者password
        params.put(HttpTlAuthentication.USERNAME,"username");
        params.put(HttpTlAuthentication.PASSWORD,"password");
        entityInfo.setParams(params);
        return entityInfo;
    }


    public static HttpEntityInfo getLogin(){

        HttpEntityInfo entityInfo = new HttpEntityInfo();
        entityInfo.setUrl("http://127.0.0.1:8097/mqtt/login/test");
        entityInfo.setMethod(HttpGet.METHOD_NAME);
        return entityInfo;
    }
}
