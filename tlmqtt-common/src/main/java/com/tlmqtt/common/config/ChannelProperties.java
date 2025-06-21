package com.tlmqtt.common.config;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 14:41
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Data
public class ChannelProperties {

    private Long writeLimit;

    private Long readLimit;

    private Long checkInterval;

    private Long maxTime;

    /**低水位线*/
    private int lowWaterMark;
    /**高水位线*/
    private int highWaterMark;
}
