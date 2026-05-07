package com.tlmqtt.sink.mysql;

import com.tlmqtt.common.sink.SinkProvider;
import com.tlmqtt.common.sink.BaseSinkEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class MySqlSinkProviderProvider implements SinkProvider {


    private final ConcurrentHashMap<String,Connection> connectionMap = new ConcurrentHashMap<>(10);


    @Override
    public void process(BaseSinkEntity baseSinkEntity, Map<String, Object> params) throws Exception{

        if(!(baseSinkEntity instanceof TlMySqlInfo)){
            return;
        }
        TlMySqlInfo entityInfo = (TlMySqlInfo) baseSinkEntity;
        String key = entityInfo.getHost()+":"+entityInfo.getPort();
        Connection connection = connectionMap.get(key);
        String sql = entityInfo.getSql();
        if(connection == null || connection.isClosed()){
            connectionMap.remove(key);
            HikariConfig config = new HikariConfig();
            String url = String.format("jdbc:mysql://%s:%s/%s", entityInfo.getHost(), entityInfo.getPort(), entityInfo.getDatabase());
            config.setJdbcUrl(url);
            config.setUsername(entityInfo.getUsername());
            config.setPassword(entityInfo.getPassword());
            config.setDriverClassName(entityInfo.getDriverClassName());
            try(HikariDataSource dataSource = new HikariDataSource(config)) {
                 connection = dataSource.getConnection();
                 connectionMap.put(key,connection);
            }
        }
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.execute();
    }
}
