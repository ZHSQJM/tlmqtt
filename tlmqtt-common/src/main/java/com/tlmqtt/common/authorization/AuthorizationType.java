package com.tlmqtt.common.authorization;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@AllArgsConstructor
@Getter
public enum AuthorizationType {

    /**
     * http
     */
    HTTP("http"),
    SQL("sql"),
    ;
    private final String contentType;
}
