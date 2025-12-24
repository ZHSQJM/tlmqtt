package com.tlmqtt.core.service;

import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.store.service.PublishService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class OfflineMessageService {

    private final PublishService publishService;
    private final ForwardMessageService forwardMessageService;

    public OfflineMessageService(PublishService publishService, ForwardMessageService forwardMessageService) {
        this.publishService = publishService;
        this.forwardMessageService = forwardMessageService;
    }

    /**
     * 触发补发逻辑
     */
    public void triggerRedelivery(TlMqttSession session, Channel channel) {
        String clientId = session.getClientId();

        // 1. 从存储中获取所有待确认/积压的消息
        publishService.findAll(clientId)
            .collectList()
            .subscribe(messages -> {
                if (messages.isEmpty()) return;

                log.info("Client [{}] reconnected, starting redelivery of {} messages", clientId, messages.size());

                for (TlMqttPublishReq msg : messages) {
                    // 2. 检查 In-Flight 窗口是否已满
                    int maxInFlight = session.getReceiveMaximum() != null ? session.getReceiveMaximum() : 65535;

                    if (session.getInFlightCount().get() >= maxInFlight) {
                        // 窗口满了，剩下的消息放入 Session 的内存队列，等待后续 ACK 驱动
                        session.getMessageQueue().offer(msg);
                        continue;
                    }

                    // 3. 执行补发
                    // 注意：离线消息在存储时已经分配过 MessageId，直接沿用
                    redeliver(session, channel, msg);
                }
            });
    }

    private void redeliver(TlMqttSession session, Channel channel, TlMqttPublishReq msg) {
        // 设置 DUP 标志（根据协议，如果是重新发送的消息，DUP 应该设为 true）
        msg.getFixedHead().setDup(true);

        // 占用窗口
        session.getInFlightCount().incrementAndGet();

        // 发送
        channel.writeAndFlush(msg).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("Failed to redeliver message to client [{}]", session.getClientId(), f.cause());
            }
        });
    }
}