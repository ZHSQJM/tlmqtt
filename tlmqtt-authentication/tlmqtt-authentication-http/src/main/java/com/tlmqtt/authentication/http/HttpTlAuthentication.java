package com.tlmqtt.authentication.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import org.apache.http.entity.ContentType;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

/**
 * 基于http接口的认证 判断依据是statusCode是200
 *
 * @author  hszhou
 */
@Slf4j
public class HttpTlAuthentication extends AbstractTlAuthentication {


    private final Gson gson = new Gson();

    /**
     * 保持与 SqlTlAuthentication 一致：使用 Map 缓存配置
     * Key 为配置对象本身或其唯一标识，Value 为该配置对应的元数据
     */
    private final Map<HttpEntityInfo, String> activeConfigs = new ConcurrentHashMap<>();

    /**
     * 全局复用的 HttpClient（连接池化）
     */
    private final CloseableHttpClient httpClient;
    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";


    public HttpTlAuthentication() {
        // 初始化高性能连接池
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // 针对 MQTT 高并发场景
        cm.setMaxTotal(500);
        cm.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
            // 建立连接超时
            .setConnectTimeout(2000)
            // 响应数据超时
            .setSocketTimeout(3000)
            .setConnectionRequestTimeout(1000)
            .build();

        this.httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    @Override
    public AuthenticationType getSupportType() {
        return AuthenticationType.HTTP;
    }

    @Override
    public boolean authenticate(String username, String password) {
        // 遍历所有已添加的 HTTP 认证源
        for (HttpEntityInfo entity : activeConfigs.keySet()) {
            try {
                if (doAuthenticate(entity, username, password)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("【TLMQTT】HttpTlAuthentication request error for URL: {}", entity.getUrl(), e);
            }
        }
        return false;
    }
    private boolean doAuthenticate(HttpEntityInfo entity, String username, String password) throws Exception {
        Map<String, String> params = entity.getParams() == null ? new HashMap<>(16) : entity.getParams();
        String userKey = params.getOrDefault(USERNAME, USERNAME);
        String passKey = params.getOrDefault(PASSWORD, PASSWORD);

        Map<String, String> bodyMap = new HashMap<>(16);
        bodyMap.put(userKey, username);
        bodyMap.put(passKey, password);

        HttpRequestBase request;
        String method = entity.getMethod();

        if (HttpGet.METHOD_NAME.equalsIgnoreCase(method)) {
            URIBuilder builder = new URIBuilder(entity.getUrl());
            bodyMap.forEach(builder::addParameter);
            request = new HttpGet(builder.build());
        } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
            HttpPost post = new HttpPost(entity.getUrl());
            String contentType = entity.getHeaders().getOrDefault(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            if (contentType.contains(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                List<NameValuePair> form = new ArrayList<>();
                bodyMap.forEach((k, v) -> form.add(new BasicNameValuePair(k, v)));
                post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));
            } else {
                post.setEntity(new StringEntity(gson.toJson(bodyMap), ContentType.APPLICATION_JSON));
            }
            request = post;
        } else {
            return false;
        }

        // 注入自定义 Header
        if (CollUtil.isNotEmpty(entity.getHeaders())) {
            entity.getHeaders().forEach(request::setHeader);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            return code == HttpStatus.SC_OK;
        }
    }

    @Override
    public void add(TlAuthenticationSubject object) {
        if (object instanceof HttpEntityInfo) {
            HttpEntityInfo info = (HttpEntityInfo) object;
            if (StrUtil.isNotBlank(info.getUrl())) {
                // 保持一致：如果不存在则添加，避免重复配置
                activeConfigs.putIfAbsent(info, info.getUrl());
                log.info("【TLMQTT】Added HTTP Auth Source: {}", info.getUrl());
            }
        }
    }

    @Override
    public void remove(TlAuthenticationSubject object) {
        if (object instanceof HttpEntityInfo) {
            HttpEntityInfo info = (HttpEntityInfo) object;
            if (StrUtil.isNotBlank(info.getUrl())) {

                activeConfigs.remove(info, info.getUrl());
                log.info("【TLMQTT】Delete HTTP Auth Source: {}", info.getUrl());
            }
        }
    }

    @Override
    public List<? extends TlAuthenticationSubject> list() {
        return new ArrayList<>(activeConfigs.keySet());
    }

    @Override
    public boolean enabled() {
        return !activeConfigs.isEmpty();
    }
}
