package com.tlmqtt.common.source;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface TlSourceProvider {





    /**
     * 初始化
     * @param sourceBean kafka信息
     * @return 初始化成功
     **/
    boolean init(AbstractTlSourceBean sourceBean);
    /**
     * 转发数据
     * @param object 数据
     **/
    void forwardData(Object object);

    /**
     * 关闭
     **/
    void close();
}
