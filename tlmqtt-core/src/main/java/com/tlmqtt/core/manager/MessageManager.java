package com.tlmqtt.core.manager;

import cn.hutool.core.util.StrUtil;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlSubClient;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.task.TlRetryTask;
import com.tlmqtt.core.task.TlSessionTask;
import com.tlmqtt.core.task.TlWillTask;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author hszhou
 */
@RequiredArgsConstructor
@Slf4j
public class MessageManager extends HashedWheelTimer {


    private static final int MAX_ID = 65535;

    private final AtomicLong counter = new AtomicLong(1);

    @Getter
    private final TlStoreManager storeManager;
    @Getter
    private final ChannelManager channelManager;
    @Getter
    private final RetryManager retryManager;

    private final ExecutorService executorService;

    // Map to track in-flight messages per client (clientId -> count)
    /**用于跟踪每个客户端已经发送中的消息个数*/
    private final Map<String, AtomicInteger> clientInFlightMessages = new ConcurrentHashMap<>();

    // Map to store receiveMaximum per client (clientId -> receiveMaximum)
    /**用于存储每个客户端的接收最大消息数*/
    private final Map<String, Integer> clientReceiveMaximums = new ConcurrentHashMap<>();

    /**用于存储每个客户端的待处理的发布消息请求*/
    private final Map<String, Queue<TlMqttPublishReq>> pendingPublishRequests = new ConcurrentHashMap<>();


    private final ConcurrentMap<String, TlWillTask> willTaskMap = new ConcurrentHashMap<>();
    /**
     * 存储别名
     */
    private final Map<String,Map<String,String>> aliasMap = new ConcurrentHashMap<>();

    public void publish(TlMqttPublishReq req,String clientId,MqttVersion mqttVersion){
        TlMqttPublishVariableHead variableHead = req.getVariableHead();
        String topic = variableHead.getTopic();
        Integer topicAlias = variableHead.getTopicAlias();
        //如果消息的客户端是5 那么需要判断topic与topicAlias是否都不为空 如果都不为空 那么就需要保存
        if (mqttVersion == MqttVersion.MQTT_5 && topicAlias!=null) {
            if (StrUtil.isNotEmpty(topic)) {
                aliasMap.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>())
                    .put(topicAlias.toString(),topic);
            } else {
                Map<String, String> clientAliases = aliasMap.get(clientId);
                if (clientAliases != null) {
                    topic = clientAliases.get(topicAlias.toString());
                }
            }

        }
        storeManager.getSubscriptionService()
                    .find(topic)
                    .doOnNext(client -> doPublish(req,client,clientId))
                    .doOnError(e -> log.error("Publish failed for topic [{}]", req.getVariableHead().getTopic(), e))
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe();

    }

    /**
     * 转发消息到各个订阅的客户端
     * @param req 原始消息
     * @param client 订阅的客户端
     * @param publishClientId 发布的客户端
     */
    private void doPublish(TlMqttPublishReq req, TlSubClient client,String publishClientId){

        TlMqttFixedHead fixedHead = req.getFixedHead();
        int sendQos =fixedHead.getQos().value();
        int subQos = client.getQos();
        //在发布和订阅的消息qos会出现降级
        int realQos = Math.min(sendQos, subQos);
        MqttQoS mqttQoS = MqttQoS.valueOf(realQos);
        String clientId = client.getClientId();
        storeManager.getSessionService()
                    .find(client.getClientId())
                    .flatMap(session -> {
                        Boolean noLocal = client.getNoLocal();
                        log.info("client=[{}],noLocal=[{}]",client.getClientId(),noLocal);
                        if(noLocal && client.getClientId().equals(publishClientId)){
                            return Mono.empty();
                        }
                        // 如果是qos0的消息 直接转发
                        TlMqttPublishReq publishReq = build(req, mqttQoS,session,client);
                        MqttVersion mqttVersion = session.getMqttVersion();
                        if(mqttVersion == MqttVersion.MQTT_5){
                            int length = publishReq.getFixedHead().getLength();
                            Integer maximumPacketSize = session.getMaximumPacketSize();
                            if (maximumPacketSize != null && length > maximumPacketSize) {
                                log.warn("Client [{}] exceeded maximum packet size, dropping message", clientId);
                                return Mono.empty();
                            }

                            int receiveMaximum = clientReceiveMaximums.computeIfAbsent(
                                clientId,
                                k -> session.getReceiveMaximum() != null ? session.getReceiveMaximum() : 65535
                            );
                            AtomicInteger inFlightCount = clientInFlightMessages.computeIfAbsent(
                                clientId,
                                k -> new AtomicInteger(0)
                            );
                            if (inFlightCount.get() >= receiveMaximum) {
                                log.debug("Client [{}] has reached receiveMaximum limit ({}), queuing message", clientId, receiveMaximum);
                                Queue<TlMqttPublishReq> queue = pendingPublishRequests.computeIfAbsent(
                                    clientId, k -> new LinkedBlockingDeque<>(1024));
                                queue.offer(publishReq);
                                //添加到队列中
                                return Mono.empty(); // Or implement a proper queue
                            }
                            inFlightCount.incrementAndGet();
                        }
                        if(mqttQoS == MqttQoS.AT_MOST_ONCE){
                            return Mono.just(publishReq);
                        }
                        return storeManager.savePublishReq(clientId, publishReq.getVariableHead().getMessageId(), publishReq);
                    })
                    .doOnError(e -> log.error("Publish failed for client [{}]", clientId, e))
                    .doOnSuccess(publishReq -> {
                        // I/O操作回到Netty线程
                        //log.info("保存到内存在的是【{}】",publishReq.getVariableHead().getMessageId());
                        send(publishReq,clientId);
                    })
                    .subscribe();

    }




     public TlMqttPublishReq build(TlMqttPublishReq req,MqttQoS mqttQoS,TlMqttSession session,TlSubClient client ){
         TlMqttPublishVariableHead variableHead = req.getVariableHead();
         TlMqttFixedHead fixedHead = req.getFixedHead();
         Integer topicAlias = variableHead.getTopicAlias();
         Short topicMaxAlias = session.getTopicMaxAlias();
         if (topicAlias != null && topicMaxAlias != null && topicAlias > topicMaxAlias) {
             log.warn("Client [{}] exceeded topic alias limit, dropping message", session.getClientId());
             //todo 服务的转发的消息主题大于客户端能接收到的最大值
             throw new RuntimeException();
         }
         MqttVersion mqttVersion = session.getMqttVersion();
         Integer subscriptionIdentifier = client.getSubscriptionIdentifier();
         //是否是保留消息
         boolean retain = fixedHead.isRetain();
         if(MqttVersion.MQTT_5==mqttVersion && !client.getRetainAsPublished() ){
             retain = false;
         }
         // 创建新的fixedHead副本，避免共享同一个对象导致的问题
         TlMqttFixedHead newFixedHead = TlMqttFixedHead.builder()
                 .messageType(req.getFixedHead().getMessageType())
                 .dup(req.getFixedHead().isDup())
                 .qos(mqttQoS)
                 .retain(retain)
                 .build();

         //从新复制一份variableHead
          TlMqttPublishVariableHead newVariableHead = TlMqttPublishVariableHead.builder()
                .topic(req.getVariableHead().getTopic())
                  .payloadFormatIndicator(req.getVariableHead().getPayloadFormatIndicator())
                  .messageExpiryInterval(req.getVariableHead().getMessageExpiryInterval())
                 //主体别名
                  .topicAlias(req.getVariableHead().getTopicAlias())
                  .responseTopic(req.getVariableHead().getResponseTopic())
                  .correlationData(req.getVariableHead().getCorrelationData())
                   .userProperties(req.getVariableHead().getUserProperties())
                    .subscriptionIdentifier(subscriptionIdentifier==null?req.getVariableHead().getSubscriptionIdentifier():subscriptionIdentifier)
                    .contentType(req.getVariableHead().getContentType())
                    .propertiesLength(req.getVariableHead().getPropertiesLength())
              .build();

         // 如果QoS不是AT_MOST_ONCE，则需要生成新的消息ID
         if (mqttQoS != MqttQoS.AT_MOST_ONCE) {
             Long messageId = nextId();
             newVariableHead.setMessageId(messageId);
         }
         TlMqttPublishReq publishReq = TlMqttPublishReq.build(
             newFixedHead,
             newVariableHead,
             req.getPayload(),
             mqttVersion);
         publishReq.setAcceptTime(req.getAcceptTime());
         return publishReq;
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


    /**
     * 客户端断开连接时调用
     * 用于清除2个
     * @param clientId 客户端ID
     **/
    public void clientDisconnected(String clientId) {
        clientInFlightMessages.remove(clientId);
        clientReceiveMaximums.remove(clientId);
        pendingPublishRequests.remove(clientId);
    }

    /**
     * 消息完成时调用
     * @param clientId 客户端ID
     **/
    public void ack(String clientId) {
        AtomicInteger inFlightCount = clientInFlightMessages.get(clientId);

        if (inFlightCount != null) {
            int newCount = inFlightCount.decrementAndGet();
            log.debug("Message completed for client [{}], in-flight count now {}", clientId, newCount);
            Queue<TlMqttPublishReq> tlMqttPublishReqs = pendingPublishRequests.get(clientId);
            if(tlMqttPublishReqs== null){
                return;
            }
            TlMqttPublishReq poll = tlMqttPublishReqs.poll();
            send(poll, clientId);
        }
    }

    private void send(TlMqttPublishReq req, String clientId) {
        if (req == null) {
            return;
        }
        Channel channel = channelManager.getChannel(clientId);
        if (channel != null && channel.isActive()) {
            MqttQoS mqttQoS = req.getFixedHead().getQos();
            channel.eventLoop().execute(() -> {
               // log.info("开始转发消息到客户端【{}】,【{}】",clientId,req);
                channel.writeAndFlush(req).addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("Failed to send message to client [{}]", clientId, future.cause());
                        return;
                    }
                    if (mqttQoS == MqttQoS.EXACTLY_ONCE || mqttQoS == MqttQoS.AT_LEAST_ONCE) {
                        Long messageId = req.getVariableHead().getMessageId();
                        TlRetryTask task = new TlRetryTask(messageId, req, channel);
                        retryManager.schedulePublishRetry(messageId, task);
                    }
                    log.info("Sent message to client [{}],消息是【{}】", clientId,req);
                });

            });
        }
    }



    /**
     * 发送遗嘱消息
     * @param clientId 客户端ID
     * @param req 发布消息
     * @param willDelayInterval 延迟消息
     */
    public Mono<Void> scheduleSendWillMessage(String clientId,TlMqttPublishReq req,int willDelayInterval){
        log.debug("遗嘱消息延迟【{}】秒发送",willDelayInterval);
        TlWillTask willTask = new TlWillTask(clientId,this,req,willDelayInterval);
        Timeout timeout = this.newTimeout(willTask,willDelayInterval, TimeUnit.SECONDS);
        willTask.setTimeout(timeout);
        willTaskMap.put(willTask.getClientId(),willTask);
        return Mono.empty();
    }


    /**
     * 取消定时删除会话
     *
     * @param clientId 客户端id
     */
    public void cancelSendWillMessage(String clientId){
        TlWillTask willTask = willTaskMap.get(clientId);
        if(willTask!=null){
            log.info("取消定时任务");
            willTask.cancel();
        }
        willTaskMap.remove(clientId);
    }

}
