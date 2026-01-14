package com.tlmqtt.authentication.sql;

import cn.hutool.core.util.StrUtil;
import com.tlmqtt.common.authentication.AbstractTlAuthentication;
import com.tlmqtt.common.authentication.AuthenticationType;
import com.tlmqtt.common.authentication.TlAuthenticationSubject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouhs
 **/
@Slf4j
public class SqlTlAuthentication extends AbstractTlAuthentication {

    private static final String DEFAULT_QUERY_SQL = "SELECT 1 FROM %s WHERE %s = ? AND %s = ? LIMIT 1";
    private final Map<SqlEntityInfo, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();
    private final Map<SqlEntityInfo, String> sqlMap = new ConcurrentHashMap<>();

    @Override
    public AuthenticationType getSupportType() {
        return AuthenticationType.SQL;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if (StrUtil.hasBlank(username, password)) {
            return false;
        }
        // 遍历所有已配置的数据源进行认证
        for (Map.Entry<SqlEntityInfo, HikariDataSource> entry : dataSourceMap.entrySet()) {
            HikariDataSource ds = entry.getValue();
            String sql = sqlMap.get(entry.getKey());

            try (Connection conn = ds.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.error("【TLMQTT】SQL Authentication attempt failed for one source", e);
            }
        }
        return false;
    }

    @Override
    public void add(TlAuthenticationSubject object) {
        if (object instanceof SqlEntityInfo) {
            SqlEntityInfo info = (SqlEntityInfo) object;
            // 如果已经存在则不重复创建
            if (dataSourceMap.containsKey(info)) {
                return;
            }
            try {
                // 优先使用当前类的加载器去加载驱动
                HikariConfig config = new HikariConfig();
                String port = StrUtil.isEmpty(info.getPort()) ? "3306" : info.getPort();
                String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", info.getHost(), port, info.getDatabase());
                config.setJdbcUrl(url);
                config.setUsername(info.getUsername());
                config.setPassword(info.getPassword());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                // 优化连接池参数
                config.setMaximumPoolSize(5);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(3000);
                HikariDataSource ds = new HikariDataSource(config);
                String sql =StrUtil.isEmpty( info.getSql())?String.format(DEFAULT_QUERY_SQL, info.getTable(), info.getUsernameColumn(), info.getPasswordColumn()):info.getSql();
                dataSourceMap.put(info, ds);
                sqlMap.put(info, sql);
                log.info("【TLMQTT】Successfully added SQL authentication source: {}", url);
            } catch (Exception e) {
                log.error("【TLMQTT】Failed to initialize SQL Data Source", e);
            }
        }
    }

    @Override
    public void remove(TlAuthenticationSubject object) {
        if (object instanceof SqlEntityInfo ) {
            SqlEntityInfo info = (SqlEntityInfo) object;
            // 优化：移除时必须调用 close() 释放连接资源，否则会导致数据库连接耗尽
            HikariDataSource ds = dataSourceMap.remove(info);
            if (ds != null && !ds.isClosed()) {
                ds.close();
                log.info("【TLMQTT】已成功移除并关闭 SQL 认证源连接池");
            }
            sqlMap.remove(info);
        }
    }

    @Override
    public List<? extends TlAuthenticationSubject> list() {
        return new ArrayList<>(dataSourceMap.keySet());

    }

    @Override
    public boolean enabled() { return !dataSourceMap.isEmpty(); }

}
