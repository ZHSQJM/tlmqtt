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
    private Long writeLimit = 104857600L;

    /**读取流量限制*/
    private Long readLimit = 52428800L;

    /**检查间隔*/
    private Long checkInterval = 1000L;

    /**最大时间*/
    private Long maxTime = 20971520L;

    /**低水位线*/
    private int lowWaterMark = 32768;

    /**高水位线*/
    private int highWaterMark = 65536;
}
