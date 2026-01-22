package com.tlmqtt.common.sink;

import lombok.Data;

/**
 * @author zhouhs
 **/

public interface DataSink {


    /**
     * 获取数据源ID
     * @return String
     */
    String getId();

    /**
     * 发送数据
     * @param data 数据
     */
    void send(String data);

    /**
     * 关闭数据源
     */
    void close();

    SinkType getType();
}
