package com.tlmqtt.core.handler;


import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.TlTopic;
import com.tlmqtt.common.model.payload.TlMqttUnSubscribePayload;
import com.tlmqtt.common.model.request.TlMqttUnSubscribeReq;
import com.tlmqtt.common.model.response.TlMqttUnSubAck;

import com.tlmqtt.store.service.SubscriptionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlUnSubscribeHandler extends AbstractTlHandler<TlMqttUnSubscribeReq> {


    public TlUnSubscribeHandler(SubscriptionService subscriptionService){
        super.setSubscriptionService(subscriptionService);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttUnSubscribeReq req, TlMqttSession session) {
        String clientId = session.getClientId();
        MqttVersion mqttVersion = session.getMqttVersion();
        int messageId = req.getVariableHead().getMessageId();
        List<TlTopic> topics = req.getPayload().getTopics();

        log.debug("Handling UNSUBSCRIBE for client: [{}], topics: {}", clientId, topics);

        // 1. 执行取消订阅逻辑并收集结果（为了 MQTT 5.0 的原因码）
        Flux.fromIterable(topics)
            .flatMap(topic ->
                subscriptionService.unsubscribe(clientId, topic.getName())
                    .map(success -> success ? MqttErrorCode.SUCCESS : MqttErrorCode.NO_SUBSCRIPTION_EXISTED)
                    .onErrorReturn(MqttErrorCode.NO_MATCHING_SUBSCRIBERS_UNSPECIFIED_ERROR)
                    .doOnNext(code -> {
                        if (code == MqttErrorCode.SUCCESS) {
                            // 同步更新 Session 内存状态
                            session.getTopics().remove(topic.getName());
                        }
                    })
            )
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(codes -> {
                // 2. 将结果转换为数组
                int[] reasonCodes = new int[codes.size()];
                for (int i = 0; i < codes.size(); i++) {
                    reasonCodes[i] = codes.get(i).byteValue();
                }

                // 3. 构建并发送 UNSUBACK
                // MQTT 3.1.1 不支持 Reason Codes，只有 MessageId
                // MQTT 5.0 需要返回每个 Topic 的处理结果
                TlMqttUnSubAck res = build(messageId, reasonCodes, mqttVersion);
                ctx.writeAndFlush(res).addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("Client [{}] successfully unsubscribed from {} topics", clientId, topics.size());
                    }
                });
            });
    }

    public static TlMqttUnSubAck build(int messageId, int[] reasonCodes, MqttVersion version) {
        if (version == MqttVersion.MQTT_5) {
            // 返回带原因码的报文
            return TlMqttUnSubAck.build (messageId, reasonCodes);
        } else {
            // 仅返回带 messageId 的报文
            return TlMqttUnSubAck.build(messageId);
        }
    }
}

