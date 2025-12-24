package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * 流量控制与整形配置
 *
 * @author hszhou
 */
@Data
public class TlChannelProperties {

    /**写入流量限制*/
    private Long writeLimit;

    /**读取流量限制*/
    private Long readLimit;

    /**检查间隔*/
    private Long checkInterval;

    /**最大时间*/
    private Long maxTime;

    /**低水位线*/
    private int lowWaterMark;

    /**高水位线*/
    private int highWaterMark;
}
