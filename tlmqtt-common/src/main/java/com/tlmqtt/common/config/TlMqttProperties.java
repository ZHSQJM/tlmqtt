package com.tlmqtt.common.config;

import lombok.Data;

/**
 * mqtt的相关配置
 *
 * @author hszhou
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
