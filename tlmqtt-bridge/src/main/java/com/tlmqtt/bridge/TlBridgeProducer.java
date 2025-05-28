package com.tlmqtt.bridge;

import com.lmax.disruptor.RingBuffer;
import com.tlmqtt.common.model.entity.PublishMessage;

/**
 * @Author: hszhou
 * @Date: 2025/5/28 10:41
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class TlBridgeProducer{

    private final RingBuffer<PublishMessage> ringBuffer;

    public TlBridgeProducer(RingBuffer<PublishMessage> ringBuffer){
       this.ringBuffer = ringBuffer;
    }

    protected void forward(PublishMessage message) {
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
}
