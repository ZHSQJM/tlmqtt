package com.tlmqtt.common.config;

import lombok.Data;

/**
 * mqtt的端口配置
 *
 * @author hszhou
 */
@Data
public class TlPortProperties {

    private int mqtt;

    private int sslMqtt;

    private int websocket;

    private int sslWebsocket;



}
