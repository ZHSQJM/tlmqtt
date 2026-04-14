package com.tlmqtt.rule.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@AllArgsConstructor
@Getter
public enum SourceEvents {


    /**当消息发送时*/
    MESSAGE(0,"消息"),
    /**当有事件发送的时候*/
    EVENT(1,"事件"),
    /**其他的mqtt服务*/
    MQTT(3,"mqtt服务")
    ;

    /**编码*/
    private final int code;

    /**名称*/
    private final String name;
}
