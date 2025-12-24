package com.tlmqtt.store.service;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.session.listener.SessionEventListener;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface ShareSubscribeService extends SessionEventListener {


    /**
     * 订阅共享订阅
     * @param client 订阅的会话
     * @return boolean
     */
    Mono<Boolean> subscribeShare(TlSubClient client);


    /**
     * 取消订阅共享订阅
     * @param client 取消订阅的会话
     * @return boolean
     */
    Mono <Boolean> unsubscribeShare(TlSubClient client);



    /**
     * 根据主题获取组对应的成员
     * @param topicName 订阅的共享订阅名称
     * @return 组对应的成员
     */
    HashMap<String, List<TlSubClient>> getGroupMember(String topicName);
}
