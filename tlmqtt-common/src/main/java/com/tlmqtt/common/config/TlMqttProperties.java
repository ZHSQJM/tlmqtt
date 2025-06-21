package com.tlmqtt.common.config;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 14:41
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Data
public class TlMqttProperties {

    private TlAuthProperties auth;

    private TlPortProperties port;

    private TlSslProperties ssl;

    private TlSessionProperties session;

    private ChannelProperties channel;

    private BusinessProperties business;


}
