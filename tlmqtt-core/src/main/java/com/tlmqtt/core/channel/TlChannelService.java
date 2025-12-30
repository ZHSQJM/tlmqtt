package com.tlmqtt.core.channel;

import io.netty.channel.Channel;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 连接管理器
 **/
public interface TlChannelService {


     /**
     * 添加客户端与通道之间的映射关系
     * @param clientId 客户端ID
     * @param newChannel 通道
     **/
     void put(String clientId, Channel newChannel);

    /**
     * 根据客户端id获取通道
     * @param clientId 客户端ID
     * @return  Channel 通道
     **/
     Channel getChannel(String clientId);


    /**
     * 移除客户端与通道之间的映射关系以及通道
     * @param clientId 客户端ID
     **/
     void remove(String clientId);
}
