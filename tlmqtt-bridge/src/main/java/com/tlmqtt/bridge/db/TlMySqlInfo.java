package com.tlmqtt.bridge.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TlMySqlInfo {

    /**ip*/
    private String host;
    /**端口号*/
    private int port;
    /**用户名*/
    private String username;
    /**密码*/
    private String password;
    /**数据库*/
    private String database;
    /**表*/
    private String table;
    /**驱动*/
    private String driverClassName;

}
