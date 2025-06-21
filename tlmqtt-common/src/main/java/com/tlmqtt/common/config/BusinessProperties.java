package com.tlmqtt.common.config;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2025/5/16 14:41
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Data
public class BusinessProperties {

    private int core;
    private int  max;
    private int  queue;
    private int  keepAlive;
}
