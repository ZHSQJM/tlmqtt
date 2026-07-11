package com.tlmqtt.store.service.impl;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import com.tlmqtt.store.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author watson
 * @created 2026/7/9
 * @Description
 */
@Slf4j
public class DefaultAuthenticationServiceImpl implements AuthenticationService {

    private final Map<AuthenticationType, List<TlAuthenticationSubject>> STORAGE = new ConcurrentHashMap<>();

    @Override
    public Map<AuthenticationType, List<TlAuthenticationSubject>> init() {
        // 返回当前所有数据的快照，用于 AuthenticationManager 启动时加载
        Map<AuthenticationType, List<TlAuthenticationSubject>> result = new HashMap<>();
        STORAGE.forEach((type, list) -> result.put(type, new ArrayList<>(list)));
        return result;
    }

    @Override
    public void save(TlAuthenticationSubject subject) {
        AuthenticationType type = subject.getAuthenticationType();
        STORAGE.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(subject);
        log.debug("【TLMQTT】Saved auth subject: type={}, id={}", type, subject.getId());
    }

    @Override
    public void delete(TlAuthenticationSubject subject) {
        AuthenticationType type = subject.getAuthenticationType();
        List<TlAuthenticationSubject> list = STORAGE.get(type);
        if (list != null) {
            list.remove(subject);
            log.debug("【TLMQTT】Deleted auth subject: type={}, id={}", type, subject.getId());
        }
    }

    @Override
    public List<TlAuthenticationSubject> getByType(AuthenticationType type) {
        return STORAGE.getOrDefault(type, Collections.emptyList());
    }
}
