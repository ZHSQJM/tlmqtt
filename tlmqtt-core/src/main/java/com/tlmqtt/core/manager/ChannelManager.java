package com.tlmqtt.core.manager;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 15:47
 * @Description: 通道管理类
 */
@Slf4j
public class ChannelManager {

    /**
     * 用户保存客户端与通道之间的映射关系
     */
    public final static ConcurrentHashMap<String, Channel> CLIENT_MAP = new ConcurrentHashMap<>(16);

    /**
     * @description: 客户端连接时 保存客户端与通道之间的映射关系以及通道保存起来
     * @author: hszhou
     * @datetime: 2025-05-08 13:43:56
     * @param: clientId
     * @param: channel
     * @return: void
     **/
    public void put(String clientId, Channel channel){
        CLIENT_MAP.putIfAbsent(clientId,  channel);
    }

    /**
     * @description: 根据客户端id获取通道
     * @author: hszhou
     * @datetime: 2025-05-08 13:44:35
     * @param:
     * @param: clientId
     * @return: Channel
     **/
    public Channel getChannel(String clientId){
        return CLIENT_MAP.get(clientId);
    }

    /**
     * @description: 移除客户端与通道之间的映射关系以及通道
     * @author: hszhou
     * @datetime: 2025-05-08 13:44:54
     * @param:
     * @param: clientId
     * @return: void
     **/
    public void remove(String clientId){
        CLIENT_MAP.remove(clientId);
    }

    /**
     * @description: 向通道中发送数据
     * @author: hszhou
     * @datetime: 2025-05-08 13:45:07
     * @param:
     * @param: clientId
     * @param: msg
     * @return: void
     **/
    public void writeAndFlush(String clientId,Object msg){
        Channel channel = getChannel(clientId);
        if(channel!=null && channel.isActive()){
            channel.writeAndFlush(msg);
        }
    }
}
