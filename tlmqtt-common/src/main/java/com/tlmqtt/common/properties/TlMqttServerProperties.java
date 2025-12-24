package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
public class TlMqttServerProperties {

    private TlAuthProperties authProperties;

    private TlPortProperties portProperties;

    private TlSslProperties sslProperties;

    private TlSessionProperties sessionProperties;

    private TlChannelProperties channelProperties;

    private TlBusinessProperties businessProperties;
}
