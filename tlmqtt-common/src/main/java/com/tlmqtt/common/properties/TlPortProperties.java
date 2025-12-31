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
    private int mqtt = 1883;

    /** ssl端口 */
    private int sslMqtt = 8883;

    /** websocket端口 */
    private int websocket = 8083;

    /** ssl websocket端口 */
    private int sslWebsocket = 8084;



}
