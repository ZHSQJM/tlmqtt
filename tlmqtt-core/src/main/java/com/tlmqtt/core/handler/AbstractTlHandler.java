package com.tlmqtt.core.handler;

import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.authorization.base.AuthorizationManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.common.interceptor.PublishInterceptor;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.ShareSubscribeService;
import com.tlmqtt.store.service.SubscriptionService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象处理器 用户处理消息
 *
 * @author hszhou
 */
@Setter
@Slf4j
public abstract class AbstractTlHandler <T extends AbstractTlMessage> extends SimpleChannelInboundHandler<T> {


    protected  SessionService sessionService;
    protected  SubscriptionService subscriptionService;
    protected  PublishService publishService;
    protected  PubrelService pubrelService;
    protected  RetainService retainService;
    protected  ShareSubscribeService shareSubscribeService;
    protected  TlChannelService channelService;
    protected  AuthenticationManager authenticationManager;
    protected  AuthorizationManager authorizationManager;
    protected  MqttConfiguration mqttConfiguration;
    protected List<PublishInterceptor> interceptors = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T req) throws Exception {

        //读取消息
        TlMqttSession session = null;
        Object mqttSession = ctx.channel().attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).get();
        if (mqttSession!= null) {
            session = (TlMqttSession) mqttSession;
        }

        if (req.getFixedHead().getMessageType() == MqttMessageType.PUBLISH && !interceptors.isEmpty()) {
            // 强转为 Publish 请求进行拦截
            TlMqttPublishReq publishReq = (TlMqttPublishReq) req;
            for (PublishInterceptor interceptor : interceptors) {
                publishReq = interceptor.intercept(ctx, publishReq, session);
                // 如果某个拦截器返回 null，表示终止后续处理和 Handler 执行
                if (publishReq == null) {
                    log.debug("Message processing terminated by interceptor: {}", interceptor.getClass().getSimpleName());
                    return;
                }
            }
            // 将处理后的结果转回 T
            req = (T) publishReq;
        }
        handle(ctx,req,session);

    }

    /**
     * 处理消息 - 由Disruptor事件处理器调用
     * @param ctx 通道
     * @param req 消息
     * @param session 会话
     */
    abstract public void handle(ChannelHandlerContext ctx, T req, TlMqttSession session);


}