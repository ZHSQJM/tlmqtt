package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;

/**
 * @author hszhou
 */
public abstract class AbstractTlMessage {


    /**
     * 获取消息类型
     * @author hszhou
     * @since  2025-05-20 18:04:39
     * @return MqttMessageType
     **/
    public abstract MqttMessageType getMessageType();
}
