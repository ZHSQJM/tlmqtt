package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
public class TlMqttServerProperties {

    /*** 认证*/
    private TlAuthProperties auth = new TlAuthProperties();
    /*** 端口*/
    private TlPortProperties port = new TlPortProperties();
    /*** SSL*/
    private TlSslProperties ssl = new TlSslProperties();
    /*** 会话*/
    private TlSessionProperties session = new TlSessionProperties();
    /*** 通道*/
    private TlChannelProperties channel = new TlChannelProperties();
    /*** 业务*/
    private TlBusinessProperties business = new TlBusinessProperties();

    /*** boss线程池大小*/
    private int bossThreadSize=1;
    /*** work线程池大小*/
    private int workThreadSize=Runtime.getRuntime().availableProcessors();
}
