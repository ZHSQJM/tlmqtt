package com.tlmqtt.authentication.sql;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * 数据库对象实体类
 *
 * @author  hszhou
 */
@AllArgsConstructor
@Data
public class SqlEntityInfo extends TlAuthenticationSubject {

    /**地址*/
    private  String host;

    /**端口号*/
    private  String port;

    /**用户名*/
    private  String username;

    /**密码*/
    private  String password;

    /**数据库名*/
    private  String  database;

    /**表名*/
    private  String table;

    /**用户名字段*/
    private  String usernameColumn;

    /**密码字段*/
    private  String passwordColumn;

    private  String driverClassName;

    /**sql*/
    private String sql;

    public SqlEntityInfo(Long id){
        setId(id);
        setAuthenticationType(AuthenticationType.SQL);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SqlEntityInfo that = (SqlEntityInfo) object;
        return Objects.equals(host, that.host) && Objects.equals(port, that.port) && Objects.equals(database,
            that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, database);
    }

}
