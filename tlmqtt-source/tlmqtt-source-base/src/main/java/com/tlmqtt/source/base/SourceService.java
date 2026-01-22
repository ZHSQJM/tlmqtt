package com.tlmqtt.source.base;

import com.tlmqtt.common.sink.DataSink;
import com.tlmqtt.common.sink.SinkType;

import java.util.HashMap;
import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 用于保存source的Item信息
 **/

public interface SourceService {


    /**
     * 加载
     * @author zhouhs
     * @return: java.util.HashMap<com.tlmqtt.common.source.SinkType,com.tlmqtt.common.source.DataSink>
     **/

    HashMap<SinkType,List<DataSink>> init();
    /**
     * 添加source
     * @param dataSink
     */
    void  add(DataSink dataSink);

    /**
     * 删除source
     * @param dataSink
     */
    void  remove(DataSink dataSink);

    /**
     * 根据sourceType获取source
     * @param sinkType
     * @return
     */
    List<? extends DataSink> listBySourceType(SinkType sinkType);

    /**
     * 分页获取source
     * @param sinkType
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<DataSink> listByPage(SinkType sinkType, int pageNum, int pageSize);

}
