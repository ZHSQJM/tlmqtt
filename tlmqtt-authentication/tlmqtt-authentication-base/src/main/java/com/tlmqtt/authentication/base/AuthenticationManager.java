package com.tlmqtt.authentication.base;


import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;
import com.tlmqtt.common.model.entity.TlAuthUser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * 认证链 只要有任何一个认证通过即可
 *
 * @author  hszhou
 */
@Slf4j
public class AuthenticationManager extends AbstractTlAuthentication {

    private final Map<String, TlAuthenticationProvider> PROVIDERS_MAP = new HashMap<>();

    private final AbstractTlAuthentication head;

    public AuthenticationManager(boolean authEnabled,List<TlAuthUser> users) {
        // 1. 初始化头部（None认证器，负责处理 "不开启认证" 的情况）
        this.head = new NoneAuthenticationService(() -> authEnabled);

        // 2. 利用 SPI 加载所有认证处理器
        List<AbstractTlAuthentication> providers = loadProviders();
        log.debug("Loaded {} AuthenticationManager providers", providers.size());
        // 3. 构建认证链
        AbstractTlAuthentication current = head;
        for (AbstractTlAuthentication provider : providers) {
            users.forEach(provider::add);
            current.setNextAuthentication(provider);
            current = provider;
            log.debug("Loaded Authentication Provider: {}", provider.getClass().getSimpleName());
        }
    }

    private List<AbstractTlAuthentication> loadProviders() {
        ServiceLoader<TlAuthenticationProvider> loader = ServiceLoader.load(TlAuthenticationProvider.class);
        List<TlAuthenticationProvider> providerList = new ArrayList<>();
        loader.forEach(providerList::add);

        loader.forEach(provider -> {
            PROVIDERS_MAP.put(provider.name(), provider);
            providerList.add( provider);
        });
        // 按 order 排序并实例化
        return providerList.stream()
            .sorted(Comparator.comparingInt(TlAuthenticationProvider::order))
            .map(TlAuthenticationProvider::create)
            .collect(Collectors.toList());
    }

    @Override
    public boolean authenticate(String username, String password) {
        // 直接从链头开始执行
        return head.execute(username, password);
    }

    @Override
    public boolean enabled() { return true; }

    @Override
    public void add(Object object) {
        AbstractTlAuthentication current = head;
        while (current != null) {
            current.add(object);
            current = current.getNextAuthentication();
        }
    }
}
