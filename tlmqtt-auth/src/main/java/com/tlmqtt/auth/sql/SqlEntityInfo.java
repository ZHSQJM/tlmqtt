package com.tlmqtt.auth.sql;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: hszhou
 * @Date: 2025/5/12 18:51
 * @Description: 数据库对象实体类
 */
@Data
@Accessors(chain = true)
public class SqlEntityInfo {

    /**地址*/
    private String host;

    /**端口号*/
    private String port;

    /**用户名*/
    private String username;

    /**密码*/
    private String password;

    /**数据库名*/
    private String  database;

    /**表名*/
    private String table;

    /**用户名字段*/
    private String usernameColumn;

    /**密码字段*/
    private String passwordColumn;

    private String driverClassName;
}
