package com.tlmqtt.common.sink;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface DataSinkProvider {

    /**
     * 初始化
     * @param config 配置信息
     * @return 初始化成功
     **/
    DataSink createSink(Map<String, Object> config);


    SinkType getType();
}
