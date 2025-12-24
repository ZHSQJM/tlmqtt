package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * 业务线程的参数
 *
 * @author hszhou
 */
@Data
public class TlBusinessProperties {

    /**
     * 核心线程数
     */
    private int corePoolSize;
    /**
     * 最大线程数
     */
    private int  maxPoolSize;
    /**
     * 队列大小
     */
    private int  queueCapacity;

    /**
     * 线程存活时间
     */
    private int  keepAliveSeconds;
}
