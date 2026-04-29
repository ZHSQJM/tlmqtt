package com.tlmqtt.rule.sink;

import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.sink.ActionSink;
import lombok.Data;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
public class MySqlSink implements ActionSink {


    private String host;
    private Integer port;
    private String root;
    private String password;
    private String database;
    private String sql;

    @Override
    public void process() {

    }
}