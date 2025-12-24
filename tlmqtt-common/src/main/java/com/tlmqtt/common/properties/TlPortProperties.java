package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * mqtt的端口配置
 *
 * @author hszhou
 */
@Data
public class TlPortProperties {

    /** mqtt端口 */
    private int mqtt;

    /** ssl端口 */
    private int sslMqtt;

    /** websocket端口 */
    private int websocket;

    /** ssl websocket端口 */
    private int sslWebsocket;



}
