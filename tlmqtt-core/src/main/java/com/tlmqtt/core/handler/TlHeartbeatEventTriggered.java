package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2024/12/24 15:39
 * @Description: 心跳时间检测
 */
@ChannelHandler.Sharable
@Slf4j
public class TlHeartbeatEventTriggered extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (!(evt instanceof IdleStateEvent)) {
            ctx.fireUserEventTriggered(evt);
            return;
        }
        Channel channel = ctx.channel();
        String clientId = channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).get().toString();
        log.debug("Handling 【Heart】 event from client:【{}】", clientId);
        // 标记为非正常断开
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);
    }
}
