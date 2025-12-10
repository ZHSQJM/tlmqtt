package com.tlmqtt.store.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.store.service.ShareSubscribeService;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class DefaultShareSubscribeServiceImpl implements ShareSubscribeService {


    /*主题与分组订阅关系*/
    /***主题对应的分组
     * topic--> topic::group/topic */
    private static final Cache<String, List<String>> TOPIC_GROUP = Caffeine.newBuilder().build();

    /**组与成员的关系
     * 组对应的成员
     *
     * */
    private static final Cache<String, List<TlSubClient>> GROUP_MEMBER = Caffeine.newBuilder().build();

    @Override
    public Mono<Boolean> subscribeShare( TlSubClient client) {
        //否则主题与分组的订阅关系就是主题与对应的组集合 组名就是主题名加组名(防止同一个组订阅了不同的主题)
        TOPIC_GROUP.get(client.getTopic(), key -> new ArrayList<>()).add(client.getGroup());
        GROUP_MEMBER.get(client.getGroup(), key -> new ArrayList<>()).add(client);
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> unsubscribeShare( TlSubClient client) {


        Objects.requireNonNull(GROUP_MEMBER.getIfPresent(client.getGroup())).removeIf(client::equals);
        if(Objects.requireNonNull(GROUP_MEMBER.getIfPresent(client.getGroup())).isEmpty()){
            GROUP_MEMBER.invalidate(client.getGroup());
        }
        if(Objects.requireNonNull(TOPIC_GROUP.getIfPresent(client.getTopic())).isEmpty()){
            TOPIC_GROUP.invalidate(client.getTopic());
        }
        return Mono.just(true);
    }

    /**
     * 获取组对应的成员
     * @param topicName 主题名称
     * @return 组与成员关系
     */
    @Override
    public HashMap<String, List<TlSubClient>> getGroupMember(String topicName){
        //表示没有找到任何组
        List<String> groupNames = TOPIC_GROUP.getIfPresent(topicName);
        if(groupNames==null){
            return null;
        }

        HashMap<String, List<TlSubClient>> groupMember = new HashMap<>();
        groupNames.forEach(groupName -> {
            List<TlSubClient> ifPresent = GROUP_MEMBER.getIfPresent(groupName);
            if(ifPresent!=null){
                groupMember.put(groupName,ifPresent);
            }
        });
        return groupMember.isEmpty()?null:groupMember;
    }
}
