package com.tlmqtt.common.config;

import lombok.Data;

/**
 * 会话超时与消息重发的延迟配置
 *
 * @author hszhou
 */
@Data
public class TlSessionProperties {

    private int timeout;

    private int delay;

    private int maxRetry;

}
