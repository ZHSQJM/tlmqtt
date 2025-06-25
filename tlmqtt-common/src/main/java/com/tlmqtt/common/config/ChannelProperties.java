package com.tlmqtt.common.config;

import lombok.Data;

/**
 * 流量控制与整形配置
 *
 * @author hszhou
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
