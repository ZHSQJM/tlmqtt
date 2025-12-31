package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * 会话超时与消息重发的延迟配置
 *
 * @author hszhou
 */
@Data
public class TlSessionProperties {

    /**
     * 会话默认的超时时间
     */
    private int timeout = 60;

    /**
     * 重发消息的延迟时间
     */
    private int delay = 10;

    /**
     * 最大重试次数
     */
    private int maxRetry = 3;

}
