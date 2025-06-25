package com.tlmqtt.auth.http;

import com.google.gson.Gson;
import com.tlmqtt.auth.AbstractTlAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

/**
 * 基于http接口的认证 判断依据是statusCode是200
 *
 * @author  hszhou
 */
@Slf4j
public class HttpTlAuthentication extends AbstractTlAuthentication {

    private final Gson gson;

    private final List<HttpEntityInfo> httpEntityInfos;

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public HttpTlAuthentication(List<HttpEntityInfo> list) {
        this.gson = new Gson();
        this.httpEntityInfos = list;
    }

    public void setHttpEntityInfos(List<HttpEntityInfo> httpEntityInfos) {
        this.httpEntityInfos.addAll(httpEntityInfos);
    }

    @Override
    public boolean authenticate(String username, String password) {

        // 2. 构造 JSON 请求体
        for (HttpEntityInfo entity : httpEntityInfos) {
            HashMap<String, String> params = entity.getParams();
            HashMap<String, String> requestParams = new HashMap<>(16);
            if (params == null) {
                params = new HashMap<>(16);
            }
            requestParams.put(params.getOrDefault(USERNAME, USERNAME), username);
            requestParams.put(params.getOrDefault(PASSWORD, PASSWORD), password);
            // 根据方法类型分发请求
            int statusCode;
            String methodName = entity.getMethod();
            if (HttpGet.METHOD_NAME.equals(methodName)) {
                statusCode = doGet(requestParams, entity.getUrl());
            } else if (HttpPost.METHOD_NAME.equals(methodName)) {
                // 获取 Content-Type 头部值
                String contentType = entity.getHeaders().getOrDefault(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
                if (contentType.contains(ContentType.APPLICATION_JSON.getMimeType())) {
                    statusCode = doPost(requestParams, entity.getUrl());
                } else if (contentType.contains(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                    statusCode = doPostForm(requestParams, entity.getUrl());
                } else {
                    statusCode = -1;
                }
            } else {
                statusCode = -1;
            }
            if (statusCode == HttpStatus.SC_OK) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void add(Object object) {
        if (object instanceof HttpEntityInfo) {
            this.httpEntityInfos.add((HttpEntityInfo) object);
        }
    }

    /**
     * 发送 POST 请求
     *
     * @param params 请求参数
     * @param url 请求地址
     * @return 响应状态码
     */
    private int doPost(HashMap<String, String> params, String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // 创建带参数的 GET 请求
            String jsonBody = gson.toJson(params);
            StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
            HttpPost request = new HttpPost(url);
            request.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
        catch (Exception e) {
            log.error("authentication http 【{}】 is error", url, e);
            return -1;
        }
    }

    /**
     * 发送 POST FROM 请求
     *
     * @param params 请求参数
     * @param url 请求地址
     * @return 响应状态码
     */
    private int doPostForm(HashMap<String, String> params, String url) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // 创建带参数的 GET 请求
            HttpPost request = new HttpPost(url);
            // 添加表单参数
            List<NameValuePair> paramsRequest = new ArrayList<>();
            params.forEach((key, value) -> paramsRequest.add(new BasicNameValuePair(key, value)));
            request.setEntity(new UrlEncodedFormEntity(paramsRequest, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
        catch (Exception e) {
            log.error("authentication http 【{}】 is error", url, e);
            return -1;
        }
    }

    /**
     * GET请求
     *
     * @param params 请求参数
     * @param url 请求地址
     * @return 响应状态码
     */
    private int doGet(HashMap<String, String> params, String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(url);
            // 添加参数
            params.forEach(uriBuilder::addParameter);
            // 构建最终的 URI
            URI uri = uriBuilder.build();
            // 创建带参数的 GET 请求
            HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
        catch (Exception e) {
            log.error("authentication http 【{}】 is error", url, e);
            return -1;
        }
    }
}
