package com.tlmqtt.common.config;

import lombok.Data;

/**
 * 业务线程的参数
 *
 * @author hszhou
 */
@Data
public class BusinessProperties {

    private int core;
    private int  max;
    private int  queue;
    private int  keepAlive;
}
