package com.tlmqtt.common.sink;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@AllArgsConstructor
@Getter
public enum SinkType {

    /**
     * http
     */
    HTTP("http"),
    SQL("sql"),
    KAFKA("kafka"),
    ;
    private final String contentType;
}
