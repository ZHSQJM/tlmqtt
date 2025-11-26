package com.tlmqtt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * mqtt5.0协议规定的消息内容类型
 * @author hszhou
 */
@AllArgsConstructor
@Getter
public enum ContentType {

    /**
     * json
     */
    JSON("application/json"),

    /**
     * xml
     */
    XML("application/xml"),

    HTML("text/html"),
    /**
     * plain
     */
    PLAIN("text/plain"),
    /**
     * csv
     */
    CSV("text/csv"),
    /**
     * octet-stream
     */
    OCTET_STREAM("application/octet-stream"),
    /**
     * 自定义协议
     */
    CUSTOM("application/custom"),

    ;
    private final String contentType;
}
