package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
public class TlMqttServerProperties {

    private TlAuthProperties auth = new TlAuthProperties();
    private TlPortProperties port = new TlPortProperties();
    private TlSslProperties ssl = new TlSslProperties();
    private TlSessionProperties session = new TlSessionProperties();
    private TlChannelProperties channel = new TlChannelProperties();
    private TlBusinessProperties business = new TlBusinessProperties();
}
