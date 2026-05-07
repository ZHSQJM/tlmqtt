package com.tlmqtt.sink.mysql;

import com.tlmqtt.common.sink.BaseSinkEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TlMySqlInfo  extends BaseSinkEntity {


    private int id;
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


}
