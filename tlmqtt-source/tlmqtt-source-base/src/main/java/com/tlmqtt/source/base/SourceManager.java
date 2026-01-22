package com.tlmqtt.source.base;

import com.tlmqtt.common.sink.SinkType;
import com.tlmqtt.common.sink.DataSink;
import com.tlmqtt.common.sink.DataSinkProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class SourceManager {


    private final Map<SinkType, DataSinkProvider> providers = new HashMap<>();

    public SourceManager(SourceService sourceService){
        // 加载 SPI 插件
        ServiceLoader.load(DataSinkProvider.class).forEach(p ->
            providers.put(p.getType(), p)
        );
    }

    /**
     * 获取支持的source类型
     * @author zhouhs
     * @return: java.util.List<com.tlmqtt.common.authentication.AuthenticationType>
     **/
    public List<SinkType> getSupportTypes(){
        return new ArrayList<>(providers.keySet());
    }


}
