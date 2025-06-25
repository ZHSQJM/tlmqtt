package com.tlmqtt.core.message;

import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryTask;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author hszhou
 */
@RequiredArgsConstructor
@Slf4j
public class TlMessageService {


    private static final int MAX_ID = 65535;
    private final AtomicLong counter = new AtomicLong(1);



    @Getter
    private final TlStoreManager storeManager;
    @Getter
    private final ChannelManager channelManager;
    @Getter
    private final RetryManager retryManager;

    private final ExecutorService executorService;

    /**
     * 用于转发消息给订阅topic的客户端
     *
     * @param topic 主题
     * @param qoS  消息
     * @param content 内容
     **/
    public void publish(String topic, MqttQoS qoS, String content) {
        //找到所有的客户端
        storeManager.getSubscriptionService().find(topic).subscribe(client -> {
            log.debug("【Client】1. Find subscribe topic 【{}】 client 【{}】",topic,client);
            doPublish(topic, qoS, content, client);
        });
    }

    /**
     * 发送消息
     *
     * @param topic 主题
     * @param qoS qos
     * @param content 内容
     * @param client 客户端
     **/
    private void doPublish(String topic, MqttQoS qoS, String content, TlSubClient client) {

        executorService.execute(()->{
            TlMqttPublishReq message = ofMessage(topic, qoS, content, client);
            String clientId = client.getClientId();
            Long messageId = message.getVariableHead().getMessageId();
            log.debug("【Find  client session 【{}】,message is 【{}】",client.getClientId(),message.getVariableHead().getMessageId());
            MqttQoS mqttQoS = message.getFixedHead().getQos();
            storeManager.getSessionService().find(clientId)
                .flatMap(session -> mqttQoS==MqttQoS.AT_MOST_ONCE
                    ?  Mono.just(message)
                    : storeManager.savePublishReq(clientId, messageId, message, mqttQoS))
                .doOnError(e -> log.error("Publish failed for client [{}]", clientId, e))
                .publishOn(Schedulers.boundedElastic())
                .subscribe(e -> {
                    // I/O操作回到Netty线程
                    Channel channel = channelManager.getChannel(clientId);
                    if (channel != null && channel.isActive()) {
                        channel.eventLoop().execute(() -> {
                            channel.writeAndFlush(message);
                            if (mqttQoS == MqttQoS.EXACTLY_ONCE ||
                                mqttQoS == MqttQoS.AT_LEAST_ONCE) {
                                TlRetryTask task = new TlRetryTask(messageId, message, channel);
                                retryManager.schedulePublishRetry(messageId, task);
                            }
                        });
                    }
                });
        });
    }
    /**
     * 构建一个新的消息
     *
     * @param topic 主题
     * @param qoS qos
     * @param content 内容
     * @param client 客户端
     * @return TlMqttPublishReq
     **/
    public TlMqttPublishReq ofMessage(String topic, MqttQoS qoS, String content, TlSubClient client) {
        int subQos = client.getQos();
        int realQos = Math.min(subQos, qoS.value());
        MqttQoS mqttQoS = MqttQoS.valueOf(realQos);
        if (mqttQoS == MqttQoS.AT_MOST_ONCE) {
            return TlMqttPublishReq.build(topic, mqttQoS, false, content, 0L);
        }
        Long messageId = nextId();
        return TlMqttPublishReq.build(topic, mqttQoS, false, content, messageId);
    }


    /**
     * 生成消息id
     *
     * @return Long
     **/
    public Long nextId() {
        long current;
        long next;
        do {
            current = counter.get();
            next = current >= MAX_ID ? 1 : current + 1;
        } while (!counter.compareAndSet(current, next));
        return current;
    }
}
