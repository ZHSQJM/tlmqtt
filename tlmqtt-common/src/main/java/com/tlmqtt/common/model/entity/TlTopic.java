package com.tlmqtt.common.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 主题类
 *
 * @author hszhou
 */
@Data
@EqualsAndHashCode
@ToString
public class TlTopic {

    @EqualsAndHashCode.Include
    private String name;

    private Integer qos;

    /**值为1 表示不能转发给自己*/
    private Boolean noLocal;

    /**表示保留消息被转发的时候 是否将保留消息位 也转发还是清除  false就是清除 true就是保留*/
    private Boolean retainAsPublished;

    /**表示保留消息是否接收  0 表示只要订阅建立，就发送保留消息 1表示 只有全新的订阅 而不是重复的订阅就发送  2 表示建立时不发送保留消息*/
    private Integer retainHandling;

}

