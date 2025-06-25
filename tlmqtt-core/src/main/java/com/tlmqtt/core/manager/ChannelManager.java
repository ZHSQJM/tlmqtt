package com.tlmqtt.core.manager;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hszhou
 */
@Slf4j
public class ChannelManager {

    /**
     * 用户保存客户端与通道之间的映射关系
     */
    public final static ConcurrentHashMap<String, Channel> CLIENT_MAP = new ConcurrentHashMap<>(16);

    /**
     * 客户端连接时 保存客户端与通道之间的映射关系以及通道保存起来
     * @param clientId 客户端ID
     * @param newChannel 通道
     **/
    public void put(String clientId, Channel newChannel){
        Channel oldChannel = CLIENT_MAP.put(clientId, newChannel);
        if(oldChannel !=null && oldChannel != newChannel && oldChannel.isActive()){
            oldChannel.close();
        }
    }

    /**
     * 根据客户端id获取通道
     * @param clientId 客户端ID
     * @return  Channel 通道
     **/
    public Channel getChannel(String clientId){
        return CLIENT_MAP.get(clientId);
    }

    /**
     * 移除客户端与通道之间的映射关系以及通道
     * @param clientId 客户端ID
     **/
    public void remove(String clientId){
        CLIENT_MAP.remove(clientId);
    }

    /**
     * 向通道中发送数据
     * @param clientId 客户端ID
     * @param msg 具体消息
     **/
    public void writeAndFlush(String clientId,Object msg){
        Channel channel = getChannel(clientId);
        if(channel!=null && channel.isActive()){
            channel.writeAndFlush(msg);
        }
    }
}
