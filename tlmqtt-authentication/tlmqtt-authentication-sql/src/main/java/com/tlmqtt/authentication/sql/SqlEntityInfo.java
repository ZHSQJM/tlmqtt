package com.tlmqtt.authentication.sql;

import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 数据库对象实体类
 *
 * @author  hszhou
 */
@EqualsAndHashCode(callSuper = true)
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
    public SqlEntityInfo(){
        setAuthenticationType(AuthenticationType.FIXED);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        SqlEntityInfo that = (SqlEntityInfo) object;
        return Objects.equals(host, that.host) && Objects.equals(port, that.port) && Objects.equals(database,
            that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, database);
    }

    public static void main(String[] args) {
        try {
            // 检查MySQL 8.x驱动类是否存在
            Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ 驱动类找到：" + driverClass.getName());
            System.out.println("✅ 类加载器：" + driverClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            System.err.println("❌ 驱动类不存在！原因：");
            e.printStackTrace();
            System.err.println("\n请检查：");
            System.err.println("1. 是否引入mysql-connector-java依赖");
            System.err.println("2. 依赖版本是否为8.x");
            System.err.println("3. 运行时类路径是否包含驱动jar");
        }
    }
}
