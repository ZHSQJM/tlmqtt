package com.tlmqtt.boot.authentication.mysql;

import com.tlmqtt.auth.sql.SqlEntityInfo;

/**
 * @Author: hszhou
 * @Date: 2025/5/29 15:48
 * @Description: mysql认证示例
 */
public class AuthenticationMysqlProvider {

    public static SqlEntityInfo providerDemo() {
        SqlEntityInfo sqlEntityInfo = new SqlEntityInfo();
        sqlEntityInfo.setHost("127.0.0.1").setPort("3306").setUsername("root").setPassword("kangni")
            .setDatabase("watson").setTable("sys_user").setUsernameColumn("username").setPasswordColumn("password")
            .setDriverClassName("com.mysql.cj.jdbc.Driver");

        return sqlEntityInfo;

    }
}