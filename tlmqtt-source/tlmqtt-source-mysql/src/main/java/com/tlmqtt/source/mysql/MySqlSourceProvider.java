package com.tlmqtt.source.mysql;

import com.tlmqtt.common.source.AbstractTlSourceBean;
import com.tlmqtt.common.source.TlSourceProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class MySqlSourceProvider implements TlSourceProvider {

    private Connection connection ;

    private String sql;
    @Override
    public boolean init(AbstractTlSourceBean mysqlInfo) {
        if(!(mysqlInfo instanceof TlMySqlInfo)){
            return false;
        }
        TlMySqlInfo entityInfo = (TlMySqlInfo) mysqlInfo;
        HikariConfig config = new HikariConfig();
        String url = String.format("jdbc:mysql://%s:%s/%s", entityInfo.getHost(), entityInfo.getPort(), entityInfo.getDatabase());
        config.setJdbcUrl(url);
        config.setUsername(entityInfo.getUsername());
        config.setPassword(entityInfo.getPassword());
        config.setDriverClassName(entityInfo.getDriverClassName());
        try(HikariDataSource dataSource = new HikariDataSource(config)) {

            this.connection = dataSource.getConnection();
            this.sql = entityInfo.getSql();
        }catch (Exception e){
            log.error("connect mysql 【{}】 fail",entityInfo.getHost(),e);
            return false;
        }
        return true;
    }

    @Override
    public void forwardData(Object object) {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
        }catch (Exception e){
            log.error("数据库执行失败",e);
        }
    }

    @Override
    public void close() {
        if(connection != null){
            try {
                connection.close();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
