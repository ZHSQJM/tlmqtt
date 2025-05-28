package com.tlmqtt.store.service;

import com.tlmqtt.common.model.entity.PublishMessage;
import reactor.core.publisher.Mono;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 15:48
 * @Description: 保留消息接口
 */
public interface RetainService {

    /**
     * 保存保留下线
     *
     * @param topic 主题
     * @param req   消息
     * @return 下线哦
     */
    Mono<Boolean> save(String topic, PublishMessage req);

    /**
     * 找到主题topic的保留消息
     *
     * @param topic 主题
     * @return 保留消息
     */
    Mono<PublishMessage> find(String topic);

    /**
     * 清除主题topic的保留消息
     *
     * @param topic 主题
     * @return 是否清除成功
     */
    Mono<Boolean> clear(String topic);
}
