package com.tlmqtt.core.retry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2025/5/8 13:34
 * @Description: 用于发送重试消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TlRetryMessage {

    private Long messageId;

    private Object message;
}
