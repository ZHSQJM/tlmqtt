package com.tlmqtt.boot.bridge;

import com.lmax.disruptor.EventHandler;
import com.tlmqtt.common.model.entity.PublishMessage;

/**
 * @Author: hszhou
 * @Date: 2025/5/28 14:57
 * @Description: 自定义桥接器
 */
public class HttpBridge implements EventHandler<PublishMessage> {
    @Override
    public void onEvent(PublishMessage event, long sequence, boolean endOfBatch) throws Exception {

    }
}
