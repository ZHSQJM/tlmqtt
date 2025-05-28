package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 10:29
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public abstract class AbstractTlMessage {


    /**
     * 获取消息类型
     * @author hszhou
     * @datetime: 2025-05-20 18:04:39
     * @return MqttMessageType
     **/
    public abstract MqttMessageType getMessageType();
}
