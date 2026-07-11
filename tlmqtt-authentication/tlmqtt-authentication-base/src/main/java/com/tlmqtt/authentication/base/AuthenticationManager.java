package com.tlmqtt.authentication.base;


import cn.hutool.core.collection.CollUtil;
import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import com.tlmqtt.common.model.entity.TlAuthUser;
import com.tlmqtt.store.service.AuthenticationService;
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
 * @author  hszhou
 */
@Slf4j
public class AuthenticationManager  {

    /** 责任链头节点 */
    private AbstractTlAuthentication head;



    /**用于快速查找某个类型的认证器实例*/
    private final Map<AuthenticationType, AbstractTlAuthentication> PROCESSOR_MAP = new HashMap<>();

    public AuthenticationManager(boolean authEnabled, List<TlAuthUser> users, AuthenticationService authenticationService) {
        // 1. 创建认证开关控制器作为链头
        NoneTlAuthentication noneAuth = new NoneTlAuthentication(authEnabled);
        // 2. SPI 加载并构建链
        buildChain(noneAuth);
        // 3. 从持久化存储加载 HTTP、SQL 等数据
        syncFromPersistence(authenticationService);

        // 4. 将配置文件中的用户注入 Fixed 认证器
        syncFixedUsers(users);
    }


    private void buildChain(AbstractTlAuthentication head ) {
        List<TlAuthenticationProvider> providers = new ArrayList<>();
        ServiceLoader.load(TlAuthenticationProvider.class).forEach(providers::add);

        // 按 order 排序
        List<TlAuthenticationProvider> sortedProviders = providers.stream()
            .sorted(Comparator.comparingInt(TlAuthenticationProvider::order))
            .collect(Collectors.toList());

        AbstractTlAuthentication current = head;
        for (TlAuthenticationProvider provider : sortedProviders) {
            AbstractTlAuthentication instance = provider.create();
            // 建立 Type -> Instance 的映射，方便后续动态添加数据
            PROCESSOR_MAP.put(provider.name(), instance);
            current.setNext(instance);
            current = instance;
        }
        this.head = head;
    }

    private void syncFixedUsers(List<TlAuthUser> users) {
        if (CollUtil.isNotEmpty(users)) {
            AbstractTlAuthentication fixed = PROCESSOR_MAP.get(AuthenticationType.FIXED);
            if (fixed != null) {
                users.forEach(fixed::add);
            }
        }
    }
    private void syncFromPersistence(AuthenticationService authService) {
        Map<AuthenticationType, List<TlAuthenticationSubject>> data = authService.init();
        data.forEach((type, subjects) -> {
            AbstractTlAuthentication processor = PROCESSOR_MAP.get(type);
            if (processor != null && subjects != null) {
                processor.init(subjects);
                log.info("【TLMQTT】Loaded {} auth subjects for type: {}", subjects.size(), type);
            }
        });
    }
    /**
     * 认证
     * @author zhouhs
     * @param: username
     * @param: password
     * @return: boolean
     **/


    public boolean authenticate(String username, String password) {
        if (head == null) {
            return true;
        }
        return head.execute(username, password);
    }

    /**
     * 获取支持的认证方式
     * @author zhouhs
     * @return: java.util.List<com.tlmqtt.common.authentication.AuthenticationType>
     **/

    public List<AuthenticationType> getSupportTypes(){
        return new ArrayList<>(PROCESSOR_MAP.keySet());
    }


    /**
     * 添加认证方法
     * @author zhouhs
     * @param: subject
     **/

    public void addSubject(TlAuthenticationSubject subject){
        AuthenticationType authenticationType = subject.getAuthenticationType();
        AbstractTlAuthentication processor = PROCESSOR_MAP.get(authenticationType);
        if (processor != null) {
            processor.add(subject);
        }
    }

    /**
     * 移除认证方式
     * @author zhouhs
     * @param: subject
     **/

    public void removeSubject(TlAuthenticationSubject subject){
        AuthenticationType authenticationType = subject.getAuthenticationType();
        AbstractTlAuthentication processor = PROCESSOR_MAP.get(authenticationType);
        if (processor != null) {
            processor.remove(subject);
        }
    }


    public List<? extends TlAuthenticationSubject> getSubjectsByType(AuthenticationType authenticationType){

        AbstractTlAuthentication processor = PROCESSOR_MAP.get(authenticationType);
        if (processor != null) {
           return processor.list();
        }
        return new ArrayList<>();
    }
}
