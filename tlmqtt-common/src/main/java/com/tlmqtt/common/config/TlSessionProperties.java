package com.tlmqtt.common.config;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2024/12/31 10:07
 * @Description: 会话超时与消息重发的延迟配置
 */
@Data
public class TlSessionProperties {

    private int timeout;

    private int delay;

}
