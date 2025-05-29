package com.tlmqtt.boot.bridge.mysql;

import com.tlmqtt.bridge.db.TlMySqlInfo;

/**
 * @Author: hszhou
 * @Date: 2025/5/29 15:28
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class MysqlProvider {


    /**
     * 表
     * @author hszhou
     * @datetime: 2025-05-29 15:33:59
     * @return TlMySqlInfo
     **/
    public static TlMySqlInfo mysqlInfo() {

        TlMySqlInfo mySqlInfo = new TlMySqlInfo();
        mySqlInfo.setHost("127.0.0.1")
            .setPort(3306)
            .setUsername("root")
            .setPassword("kangni")
            .setDatabase("watson")
            .setTable("mqtt_msg")
           .setDriverClassName("com.mysql.cj.jdbc.Driver");
        return mySqlInfo;
    }
}
