package com.tlmqtt.auth.sql;

import com.tlmqtt.auth.AbstractTlAuthentication;
import com.tlmqtt.common.exception.TlAuthenticationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基于数据的用户名密码认证
 *
 * @author  hszhou
 */
@Slf4j
public class SqlTlAuthentication extends AbstractTlAuthentication {

    private static final String SQL = "SELECT COUNT(*) FROM %s WHERE %s = ? AND %s = ?";

    private final List<SqlEntityInfo> sqlEntityInfos;

    public SqlTlAuthentication(List<SqlEntityInfo> list) {
        this.sqlEntityInfos = list;
    }

    @Override
    public boolean authenticate(String username, String password) {

        for (SqlEntityInfo entityInfo : sqlEntityInfos) {
            HikariConfig config = new HikariConfig();
            String url = String.format("jdbc:mysql://%s:%s/%s", entityInfo.getHost(), entityInfo.getPort(),
                entityInfo.getDatabase());
            config.setJdbcUrl(url);
            config.setUsername(entityInfo.getUsername());
            config.setPassword(entityInfo.getPassword());
            config.setDriverClassName(entityInfo.getDriverClassName());
            try (HikariDataSource dataSource = new HikariDataSource(config)) {
                String sql = String.format(SQL, entityInfo.getTable(), entityInfo.getUsernameColumn(),
                    entityInfo.getPasswordColumn());
                try (Connection connection = dataSource.getConnection()) {
                    if (connection != null) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                            preparedStatement.setString(1, username);
                            preparedStatement.setString(2, password);
                            try (ResultSet rs = preparedStatement.executeQuery()) {
                                if (rs.next()) {
                                    int count = rs.getInt(1);
                                    if (count > 0) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new TlAuthenticationException();
            }
        }

        return false;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void add(Object object) {
        if (object instanceof SqlEntityInfo) {
            this.sqlEntityInfos.add((SqlEntityInfo) object);
        }
    }

    public void setSqlEntityInfos(Supplier<List<SqlEntityInfo>> entityInfos) {
        this.sqlEntityInfos.addAll(entityInfos.get());
    }

}
