package com.tlmqtt.core;

import com.tlmqtt.common.config.*;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 10:55
 * @Description: mqtt的服务类
 */
@Slf4j
@Getter
public class TlMqttServer extends AbstractTlServer {



    public TlMqttServer() {
        super();
        MqttConfiguration mqttConfiguration = new MqttConfiguration();
        TlMqttProperties mqttProperties = mqttConfiguration.getMqttProperties();
        TlPortProperties port = mqttProperties.getPort();
        setPort(port.getMqtt());
        TlSslProperties sslProperties = mqttProperties.getSsl();
        boolean ssl = sslProperties.isEnabled();
        if (ssl) {
            int sslMqtt = port.getSslMqtt();
            setPort(sslMqtt);
            setCertPath(sslProperties.getCertPath());
            setPrivatePath(sslProperties.getPrivatePath());
        }
    }


    @Override
    public void addPipeline(ChannelPipeline pipeline) {

    }

}
