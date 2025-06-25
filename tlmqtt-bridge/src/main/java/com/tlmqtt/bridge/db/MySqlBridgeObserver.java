package com.tlmqtt.bridge.db;

import com.lmax.disruptor.EventHandler;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库桥接数据
 *
 * @author hszhou
 */
@Slf4j
public class MySqlBridgeObserver  implements EventHandler<PublishMessage> {

    private final List<TlMySqlInfo> list = new ArrayList<>();

    private final ConcurrentHashMap<String, Connection> connectPool = new ConcurrentHashMap<>();

    private final static String SQL = "INSERT INTO %s ( message_id, topic, client_id, message, qos, retain, dup) VALUES( '%s', '%s', '%s', '%s', '%s', %s,%s)";

    /**
     * 添加数据库连接信息
     * @param object TlMySqlInfo
     */
    public void add(Object object) {
        if(object instanceof TlMySqlInfo){
            TlMySqlInfo entityInfo = (TlMySqlInfo) object;
            HikariConfig config = new HikariConfig();
            String url = String.format("jdbc:mysql://%s:%s/%s", entityInfo.getHost(), entityInfo.getPort(), entityInfo.getDatabase());
            config.setJdbcUrl(url);
            config.setUsername(entityInfo.getUsername());
            config.setPassword(entityInfo.getPassword());
            config.setDriverClassName(entityInfo.getDriverClassName());
            HikariDataSource dataSource = new HikariDataSource(config);
            try {
                Connection connection = dataSource.getConnection();
                connectPool.put(entityInfo.getHost()+entityInfo.getPort()+entityInfo.getTable(),connection);
            }catch (Exception e){
                log.error("connect mysql 【{}】 fail",entityInfo.getHost(),e);
            }
            this.list.add(entityInfo);
        }

    }

    /**
     * 数据库桥接数据
     * @param event PublishMessage
     * @param sequence long
     * @param endOfBatch boolean
     * @throws Exception 异常
     */
    @Override
    public void onEvent(PublishMessage event, long sequence, boolean endOfBatch) throws Exception {
        for (TlMySqlInfo entityInfo : list) {
            Connection connection = connectPool.get(entityInfo.getHost() + entityInfo.getPort()+entityInfo.getTable());
            if(connection!=null){
                String sql = String.format(SQL, entityInfo.getTable(), event.getMessageId(), event.getTopic(), event.getClientId(),event.getMessage(), event.getQos(), event.isRetain(), event.isDup());
                try{
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.execute();
                }catch (Exception e){
                    log.error("数据库执行失败",e);
                }
            }

        }
    }
}
