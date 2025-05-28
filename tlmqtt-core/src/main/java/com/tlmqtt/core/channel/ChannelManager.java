package com.tlmqtt.core.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
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
     * 用于保存所有的通道
     */
    public static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户保存客户端与通道之间的映射关系
     */
    public final static ConcurrentHashMap<String, ChannelId> CLIENT_MAP = new ConcurrentHashMap<>(16);

    /**
     * @description: 客户端连接时 保存客户端与通道之间的映射关系以及通道保存起来
     * @author: hszhou
     * @datetime: 2025-05-08 13:43:56
     * @param: clientId
     * @param: channel
     * @return: void
     **/
    public void put(String clientId, Channel channel){
        log.debug("client 【{}】 channel save",clientId);
        ChannelId oldId = CLIENT_MAP.put(clientId, channel.id());
        if (oldId != null) {
            Channel oldChannel = CHANNEL_GROUP.find(oldId);
            if (oldChannel != null) {
                oldChannel.close();
            }
        }
        CHANNEL_GROUP.add(channel);

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
        ChannelId channelId = CLIENT_MAP.get(clientId);
        if (channelId == null) {
            return null;
        }
        Channel channel = CHANNEL_GROUP.find(channelId);
        if (channel == null) {
            // 自动清理无效条目
            CLIENT_MAP.remove(clientId);
            return null;
        }
        return channel;
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
        ChannelId channelId = CLIENT_MAP.remove(clientId);
        if (channelId != null) {
            CHANNEL_GROUP.remove(channelId);
        }
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
