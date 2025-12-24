package com.tlmqtt.authorization.base;

import com.tlmqtt.common.authorization.TlAuthorizationProvider;
import com.tlmqtt.common.model.TlMqttSession;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * @author  hszhou
 */
@Slf4j
public class AuthorizationManager {

    private final List<TlAuthorizationProvider> providers;

    public AuthorizationManager() {
        // 动态加载 Classpath 下所有的 TlAuthorizationProvider 实现
        this.providers = loadProviders();
        log.info("Loaded {} ACL Authorization providers", providers.size());
    }

    private List<TlAuthorizationProvider> loadProviders() {
        ServiceLoader<TlAuthorizationProvider> loader = ServiceLoader.load(TlAuthorizationProvider.class);
        List<TlAuthorizationProvider> list = new ArrayList<>();
        for (TlAuthorizationProvider provider : loader) {
            list.add(provider);
        }
        // 按 order 排序，先校验高优先级的
        return list.stream()
            .sorted(Comparator.comparingInt(TlAuthorizationProvider::order))
            .collect(Collectors.toList());
    }

    /**
     * 订阅校验：只要有一个 Provider 返回 false，则拒绝订阅
     */
    public boolean checkSubscribePermission(String clientId, String username, String ip, String topic) {
        if (providers.isEmpty()) {
            // 如果没有配置任何授权器，默认允许（或根据业务改为默认禁止）
            return true;
        }

        for (TlAuthorizationProvider provider : providers) {
            if (!provider.checkSubscribePermission(clientId, username, ip, topic)) {
                log.warn("ACL Deny [Subscribe]: Client={}, Topic={}, Provider={}",
                    clientId, topic, provider.getClass().getSimpleName());
                return false;
            }
        }
        return true;
    }

    /**
     * 发布校验：只要有一个 Provider 返回 false，则拒绝发布
     */
    public boolean checkPublishPermission(String clientId, String username, String ip, String topic) {
        if (providers.isEmpty()) {
            return true;
        }

        for (TlAuthorizationProvider provider : providers) {
            if (!provider.checkPublishPermission(clientId, username, ip, topic)) {
                log.warn("ACL Deny [Publish]: Client={}, Topic={}, Provider={}",
                    clientId, topic, provider.getClass().getSimpleName());
                return false;
            }
        }
        return true;
    }

    public boolean checkSubscribePermission(TlMqttSession session, String topic) {
        return checkSubscribePermission(
            session.getClientId(),
            session.getUsername(),
            session.getIp(),
            topic
        );
    }
}
