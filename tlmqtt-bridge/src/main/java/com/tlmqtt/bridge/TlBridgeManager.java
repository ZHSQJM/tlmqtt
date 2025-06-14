package com.tlmqtt.bridge;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.tlmqtt.bridge.db.MySqlBridgeObserver;
import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.KafkaBridgeObserver;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;
import com.tlmqtt.common.model.entity.PublishMessage;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @Author: hszhou
 * @Date: 2025/5/12 15:02
 * @Description: 桥接管理器
 */
@Slf4j
public class TlBridgeManager {

    public final Disruptor<PublishMessage> disruptor;

    private final KafkaBridgeObserver kafkaBridgeObserver;

    private final MySqlBridgeObserver mySqlBridgeObserver;

    private final   RingBuffer<PublishMessage> ringBuffer;

    public TlBridgeManager() {
        disruptor = new Disruptor<>(PublishMessage::new, 16, new DefaultThreadFactory("tl-mqtt-bridge"), ProducerType.MULTI,new YieldingWaitStrategy());
        kafkaBridgeObserver = new KafkaBridgeObserver();
        mySqlBridgeObserver = new MySqlBridgeObserver();
        disruptor.handleEventsWith(kafkaBridgeObserver,mySqlBridgeObserver);
        ringBuffer = disruptor.getRingBuffer();
        disruptor.start();
    }

    public void send(PublishMessage message){
        long sequence = ringBuffer.next();
        try {
            PublishMessage publishMessage = ringBuffer.get(sequence);
            publishMessage.setMessageId(message.getMessageId());
            publishMessage.setTopic(message.getTopic());
            publishMessage.setClientId(message.getClientId());
            publishMessage.setMessage(message.getMessage());
            publishMessage.setQos(message.getQos());
            publishMessage.setRetain(message.isRetain());
            publishMessage.setDup(message.isDup());
        }finally {
            ringBuffer.publish(sequence);
        }
    }

    public void addMysqlInfo(TlMySqlInfo tlMySqlInfo) {
        mySqlBridgeObserver.add(tlMySqlInfo);
    }

    public void addKafkaInfo(TlKafkaInfo kafkaInfo) {
        kafkaBridgeObserver.add(kafkaInfo);
    }

}
