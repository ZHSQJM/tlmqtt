package com.tlmqtt.core.handler;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.core.disruptor.DisruptorManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象处理器 用户处理消息
 *
 * @author hszhou
 */
@Slf4j
public abstract class AbstractTlHandler <T extends AbstractTlMessage> extends SimpleChannelInboundHandler<T> {
    
    private static DisruptorManager disruptorManager;
    
    static {
        // 初始化Disruptor管理器
        disruptorManager = new DisruptorManager();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T req) throws Exception {
        TlMqttSession session = null;
        Object o = ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        if (o != null) {
            session = (TlMqttSession) o;
        }

        handle(ctx,req,session);
        // 将业务处理交给Disruptor队列完成
        //disruptorManager.publishEvent(ctx, req, session);
    }

    /**
     * 处理消息 - 由Disruptor事件处理器调用
     * @param ctx 通道
     * @param req 消息
     * @param session 会话
     */
    abstract public void handle(ChannelHandlerContext ctx, T req, TlMqttSession session);
    
    /**
     * 在程序关闭时调用，用于释放Disruptor资源
     */
    public static void shutdown() {
        if (disruptorManager != null) {
            disruptorManager.shutdown();
        }
    }



}