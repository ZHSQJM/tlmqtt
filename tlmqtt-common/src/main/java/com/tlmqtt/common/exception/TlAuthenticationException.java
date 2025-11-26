package com.tlmqtt.common.exception;

import com.tlmqtt.common.enums.MqttVersion;

/**
 * 认证失败异常
 *
 * @author hszhou
 */
public class TlAuthenticationException extends RuntimeException{


    /**
     * mqtt的协议版本 3.1.1 or 5
     * 不同的认证失败处理不一样
     */
    private final MqttVersion mqttVersion;


    public TlAuthenticationException(MqttVersion mqttVersion){
        this.mqttVersion = mqttVersion;
    }
}
