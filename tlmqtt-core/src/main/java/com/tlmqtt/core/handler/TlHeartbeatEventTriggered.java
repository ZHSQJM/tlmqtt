package com.tlmqtt.core.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;


/**
 * @Author: hszhou
 * @Date: 2024/12/24 15:39
 * @Description: 心跳时间检测
 */
@ChannelHandler.Sharable
public class TlHeartbeatEventTriggered extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if(evt instanceof IdleStateEvent){

        }
        super.userEventTriggered(ctx, evt);
    }
}
