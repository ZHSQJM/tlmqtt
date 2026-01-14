package com.tlmqtt.authentication.base;


import cn.hutool.core.collection.CollUtil;
import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AbstractAuthenticationService;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationProvider;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
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
 * @author  hszhou
 */
@Slf4j
public class AuthenticationManager  {

    private final AbstractTlAuthentication head;
    /**用于快速查找某个类型的认证器实例*/
    private final Map<AuthenticationType, AbstractTlAuthentication> PROCESSOR_MAP = new HashMap<>();

    public AuthenticationManager(boolean authEnabled,List<TlAuthUser> users, AbstractAuthenticationService authService) {
        // 1. 初始化空头（处理全局开关）
        this.head = new NoneAuthenticationService(() -> authEnabled);
        // 2. SPI 加载并构建链
        loadAndBuildChain();
        // 3. 核心：将 Service 中的数据同步到对应的处理器中
        syncDataFromService(authService,users);
    }


    private void loadAndBuildChain() {
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
    }

    private void syncDataFromService(AbstractAuthenticationService authService,List<TlAuthUser> users) {
        // 初始化加载 Service 里的数据
        Map<AuthenticationType, List<TlAuthenticationSubject>> data = authService.init();
        data.forEach((type, subjects) -> {
            AbstractTlAuthentication processor = PROCESSOR_MAP.get(type);
            if (processor != null) {
                subjects.forEach(processor::add);
            }
        });

        if(CollUtil.isNotEmpty(users)){
            AbstractTlAuthentication abstractTlAuthentication = PROCESSOR_MAP.get(AuthenticationType.FIXED);
            if(abstractTlAuthentication != null){
                users.forEach(abstractTlAuthentication::add);
            }

        }

    }

    /**
     * 认证
     * @author zhouhs
     * @param: username
     * @param: password
     * @return: boolean
     **/


    public boolean authenticate(String username, String password) {
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
