package com.tlmqtt.core.manager;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.task.TlSessionTask;
import com.tlmqtt.store.service.*;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
@Setter
@Getter
public class TlStoreManager extends HashedWheelTimer {

    private final ConcurrentMap<String, TlSessionTask> sessionTaskMap = new ConcurrentHashMap<>();

    private SessionService sessionService;

    private SubscriptionService subscriptionService;

    private PublishService publishService;

    private PubrelService pubrelService;

    private RetainService retainService;


    /**
     * 构造函数
     * @param sessionService session服务
     * @param subscriptionService 订阅服务
     * @param publishService 发布服务
     * @param pubrelService pubrel服务
     * @param retainService retain服务
     **/
    public TlStoreManager(SessionService sessionService, SubscriptionService subscriptionService, PublishService publishService,
        PubrelService pubrelService,RetainService retainService) {
        this.sessionService = sessionService;
        this.subscriptionService = subscriptionService;
        this.publishService = publishService;
        this.pubrelService = pubrelService;
        this.retainService = retainService;
    }


    /**
     * 保存publish消息
     * @param clientId 客户端ID
     * @param messageId 消息ID
     * @return Mono 保存成功返回消息体
     **/
    public Mono<TlMqttPublishReq> savePublishReq(String clientId, Long messageId, TlMqttPublishReq publishReq) {
        log.debug("save publish messageId is【{}】", messageId);
        return publishService.save(clientId, messageId, publishReq);
    }



    /**
     * 清除客户端所有的信息

     * @param clientId 客户端
     * @return void 清除成功返回void
     **/
    public Mono<Void> clearAll(String clientId) {

        //log.info("清除会话");
       return Mono.when(
            sessionService.clear(clientId),
            subscriptionService.clear(clientId),
            publishService.clearAll(clientId),
            publishService.clearWill(clientId),
            pubrelService.clearAll(clientId));
    }


    /**
     * 定时删除会话
     *
     * @param session 客户端id
     */
    public Mono<Void> scheduleRemoveSession(TlMqttSession session){

        TlSessionTask sessionTask = new TlSessionTask(session.getClientId(),this);
        Timeout timeout = this.newTimeout(sessionTask, session.getSessionExpiryInterval(), TimeUnit.SECONDS);
        sessionTask.setTimeout(timeout);
        sessionTaskMap.put(session.getClientId(),sessionTask);
        return Mono.empty();
    }


    /**
     * 取消定时删除会话
     *
     * @param clientId 客户端id
     */
    public void cancelRemoveSession(String clientId){
        TlSessionTask sessionTask = sessionTaskMap.get(clientId);
        if(sessionTask!=null){
            log.info("取消定时任务");
            sessionTask.cancel();
        }
        sessionTaskMap.remove(clientId);
    }


}
