package com.tlmqtt.common.sink;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@FunctionalInterface
public interface SinkProvider {


    /**
     * 收到消息的处理
     * @author zhouhs
     * @param: config
     * @param: params
     **/

    void process(BaseSinkEntity sinkEntity, Map<String,Object> params) throws Exception;
}
