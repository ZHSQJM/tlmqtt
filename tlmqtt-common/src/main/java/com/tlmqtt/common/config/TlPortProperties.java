package com.tlmqtt.common.config;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2024/12/31 10:07
 * @Description: mqtt的端口配置
 */
@Data
public class TlPortProperties {

    private int mqtt;

    private int sslMqtt;

    private int websocket;

    private int sslWebsocket;



}
