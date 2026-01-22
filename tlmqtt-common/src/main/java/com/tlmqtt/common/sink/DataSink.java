package com.tlmqtt.common.sink;

import lombok.Data;

/**
 * @author zhouhs
 **/

public interface DataSink {


    String getId();

    void send(String data);

    void close();

    SinkType getType();
}
