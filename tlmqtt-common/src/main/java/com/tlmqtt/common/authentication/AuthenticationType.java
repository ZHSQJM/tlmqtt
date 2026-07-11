package com.tlmqtt.common.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@AllArgsConstructor
@Getter
public enum AuthenticationType {

    /**
     * http
     */
    HTTP("HTTP"),
    /**
     * SQL
     */
    SQL("SQL"),

    /**
     * FIXED
     */
    FIXED("FIXED"),
    ;
    private final String type;
}
