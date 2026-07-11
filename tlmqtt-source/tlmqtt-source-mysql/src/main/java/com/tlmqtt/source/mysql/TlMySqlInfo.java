package com.tlmqtt.source.mysql;

import com.tlmqtt.common.sink.DataSink;
import com.tlmqtt.common.sink.SinkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TlMySqlInfo implements DataSink {

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
    /**sql*/
    private String sql;

    public String getId() {
        return null;
    }

    public void send(String data) {

    }

    public void close() {

    }

    public SinkType getType() {
        return null;
    }
}
