package com.tlmqtt.core.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Disruptor管理器，用于处理MQTT消息事件
 */
@Slf4j
public class DisruptorManager {

    private final Disruptor<TlMqttEvent> disruptor;

    @Getter
    private static DisruptorManager instance;

    public DisruptorManager() {
        int bufferSize = 1024 * 8;
        WaitStrategy waitStrategy = new BlockingWaitStrategy();
        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        this.disruptor = new Disruptor<>(
                TlMqttEvent::new,
                bufferSize,
                threadFactory,
                ProducerType.MULTI,
                waitStrategy);

//        WorkHandler<TlMqttEvent>[] handlers = new WorkHandler[]{
//                new TlMqttEventHandler()
//        };
//
//        // 注册事件处理器
//        this.disruptor.handleEventsWithWorkerPool(handlers);
        this.disruptor.start();

        instance = this;
    }

    public void publishEvent(ChannelHandlerContext ctx, AbstractTlMessage message, TlMqttSession session) {
        disruptor.publishEvent((event, sequence) -> {
            event.setCtx(ctx);
            event.setMessage(message);
            event.setSession(session);
        });
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}