package com.tlmqtt.core.handler;

import com.tlmqtt.auth.AuthenticationManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttConnectReturnCode;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.PublishMessage;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.request.TlMqttConnectReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnack;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.retry.TlRetryTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hszhou
 * @Date: 2025/6/5 18:50
 * @Description: 连接处理器
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlConnectHandler extends SimpleChannelInboundHandler<TlMqttConnectReq>{

    private final TlStoreManager storeManager;

    private final ChannelManager channelManager;

    private final AuthenticationManager authenticationManager;

    private final RetryManager retryManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttConnectReq req) throws Exception {
        Channel channel = ctx.channel();
        log.debug("Handling 【CONNECT】 event from client:【{}】", req.getPayload().getClientId());
        if(channel.hasAttr(AttributeKey.valueOf(Constant.CLIENT_ID))){
            log.warn("Client:【{}】 has already connected", req.getPayload().getClientId());
            sendConnack(channel, req.getVariableHead().getCleanSession(),MqttConnectReturnCode.CONNECTION_REFUSED_PROTOCOL_ERROR);
            channel.close();
            return;
        }
        if (!preCheck(req, channel)) {
            return;
        }
        if (!authenticate(req)) {
            log.error("Authentication failed for client:【{}】", req.getPayload().getClientId());
            sendConnack(channel, req.getVariableHead().getCleanSession(),
                MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
            return;
        }
        //注册客户端连接
        registerClient(req, channel);
        handlerSession(req, ctx)
            .then(handleWillMessage(req))
            .subscribe();
    }

    /**
     * @description: 预检查
     * @author: hszhou
     * @datetime: 2025-04-28 16:48:35
     * @param: req 连接检查
     * @param: channel
     * @return: boolean
     */
    private boolean preCheck(TlMqttConnectReq req, Channel channel) {
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        //空指针校验
        if (variableHead == null || req.getPayload() == null) {
            return false;
        }

        // 2. 协议版本检查
        if (!validateProtocolVersion(variableHead.getProtocolVersion(), channel)) {
            sendConnack(channel, variableHead.getCleanSession(),
                MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            return false;
        }

        if (variableHead.getReserved() != 0) {
            log.error("Invalid reserved bits from client:【{}】", req.getPayload().getClientId());
            return false;
        }

        return true;
    }

    /**
     * 用户名密码校验
     *
     * @param req 连接
     * @return boolean
     * @author hszhou
     * @datetime: 2025-06-09 13:52:04
     **/
    private boolean authenticate(TlMqttConnectReq req) {
        TlMqttConnectPayload payload = req.getPayload();
        return authenticationManager.authenticate(payload.getUsername(), payload.getPassword());
    }

    /**
     * 会话处理
     *
     * @param req 连接信息b
     * @param ctx 通道
     * @return Mono<Void>
     * @author hszhou
     * @datetime: 2025-06-09 13:53:04
     **/
    private Mono<Boolean> handlerSession(TlMqttConnectReq req, ChannelHandlerContext ctx) {
        String clientId = req.getPayload().getClientId();
        final String username = req.getPayload().getUsername();
        boolean cleanSession = req.getVariableHead().getCleanSession() != 0;

        return Mono.defer(() -> cleanSession ? storeManager.clearAll(clientId) : Mono.empty())
            .then(storeManager.getSessionService().find(clientId))
            .switchIfEmpty(Mono.defer(() -> createNewSession(clientId, cleanSession, username, ctx)))
            .flatMap(session -> completeSessionHandling(session, req, ctx, cleanSession))
            .doOnSuccess(e -> setupHeartBeat(ctx, req.getVariableHead().getKeepAlive()));
    }

    /**
     * @param session 会话
     * @param req 连接信息
     * @param ctx 通道
     * @param cleanSession 是否清除会话
     * @return Mono<Boolean>
     * @description: 会话处理完成
     * @author: hszhou
     * @datetime: 2025-06-09 13:54:04
     **/
    private Mono<Boolean> completeSessionHandling(TlMqttSession session, TlMqttConnectReq req,
        ChannelHandlerContext ctx, boolean cleanSession) {
        String clientId = req.getPayload().getClientId();
        sendConnack(ctx.channel(), cleanSession ? 1 : 0, MqttConnectReturnCode.CONNECTION_ACCEPTED);
        if (!cleanSession) {
            republish(clientId, ctx.channel());
            session.setTopics(new HashSet<>());
            session.setCleanSession(false);
        }
        return storeManager.getSessionService().save(session);
    }

    /**
     * 创建新的会话
     *
     * @param clientId 客户端
     * @param cleanSession 是否保留会话
     * @param username 用户名
     * @return Mono<TlMqttSession>
     * @author hszhou
     * @datetime: 2025-06-09 15:13:47
     **/
    private Mono<TlMqttSession> createNewSession(String clientId, boolean cleanSession, String username,
        ChannelHandlerContext ctx) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String hostAddress = remoteAddress.getAddress().getHostAddress();
        return Mono.just(TlMqttSession.build(clientId, cleanSession, username, hostAddress));
    }

    /**
     * 添加心跳
     *
     * @param ctx 通道
     * @param keepAlive 心跳间隔
     * @author hszhou
     * @datetime: 2025-06-09 15:13:25
     **/
    private void setupHeartBeat(ChannelHandlerContext ctx, short keepAlive) {
        ctx.pipeline().addLast(new IdleStateHandler(0, 0, keepAlive, TimeUnit.SECONDS));
    }

    /**
     * 处理遗嘱消息
     *
     * @param req 连接报文
     * @return Mono<Void>
     * @author hszhou
     * @datetime: 2025-06-09 14:09:44
     **/
    private Mono<Void> handleWillMessage(TlMqttConnectReq req) {
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        if (variableHead.getWillFlag() != 1) {
            return Mono.empty();
        }
        return saveWillMessage(req.getPayload().getWillTopic(), MqttQoS.valueOf(variableHead.getWillQos()),
            variableHead.getWillRetain() != 0, req.getPayload().getWillMessage(),
            req.getPayload().getClientId()).then();
    }

    /**
     * 回送通知
     *
     * @param channel 通道
     * @param cleanSession 是否清除会话
     * @param returnCode 响应码
     * @author hszhou
     * @datetime: 2025-06-09 14:10:11
     **/
    private void sendConnack(Channel channel, int cleanSession, MqttConnectReturnCode returnCode) {
        channel.writeAndFlush(TlMqttConnack.build(cleanSession, returnCode));
    }

    /**
     * @description: 存储遗嘱消息
     * @author: hszhou
     * @datetime: 2025-04-28 16:48:35
     * @param:
     * @param: willTopic 遗嘱消息主题
     * @param: qoS 遗嘱消息qos
     * @param: isRetain 是否是保留消息
     * @param: messageContent 遗嘱消息内容
     * @param: clientId 客户端ID
     * @return: Mono<Boolean>
     **/
    private Mono<Boolean> saveWillMessage(String willTopic, MqttQoS qoS, boolean isRetain, String content,
        String clientId) {
        PublishMessage message = PublishMessage.build(0L, willTopic, clientId, content, qoS.value(), isRetain, false);
        Mono<Boolean> savePublisher = storeManager.getPublishService().saveWill(clientId, message);
        Mono<Boolean> retainPublisher = isRetain
            ? storeManager.getRetainService().save(willTopic, message)
            : Mono.empty();
        return savePublisher.then(retainPublisher);
    }

    /**
     * 校验协议版本是否正确
     *
     * @param protocolVersion 协议版本
     * @param channel 通道
     * @return boolean
     * @author hszhou
     * @datetime: 2025-06-09 15:12:48
     **/
    private boolean validateProtocolVersion(Short protocolVersion, Channel channel) {
        if (!(protocolVersion.toString().equals(MqttVersion.MQTT_5.protocolLevel() + "") || protocolVersion.toString()
            .equals(MqttVersion.MQTT_3_1_1.protocolLevel() + ""))) {
            sendConnack(channel, 0, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            return false;
        }
        return true;
    }

    /**
     * 注册客户端信息到channel中
     *
     * @param req connect报文
     * @param channel 通道
     * @author hszhou
     * @datetime: 2025-06-09 15:10:48
     **/
    private  void registerClient(TlMqttConnectReq req, Channel channel) {
        String clientId = req.getPayload().getClientId();

        // 先设置channel属性
        channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).set(clientId);
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        channel.attr(AttributeKey.valueOf(Constant.IP)).set(socketAddress.getAddress().getHostAddress());
        channel.attr(AttributeKey.valueOf(Constant.USERNAME)).set(req.getPayload().getUsername());
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);

        // 注册到ChannelManager（会处理旧连接）
        channelManager.put(clientId, channel);

    }

    /**
     * 重发消息
     *
     * @param clientId 客户端ID
     * @author hszhou
     * @datetime: 2025-06-09 14:12:24
     **/
    private void republish(String clientId, Channel channel) {

        Flux.merge(storeManager.getPublishService().findAll(clientId)
            .doOnNext(msg -> log.debug("Resending PUBLISH message 【{}】", msg.getMessageId())).flatMap(msg -> {
                Long messageId = msg.getMessageId();
                TlMqttPublishReq req = TlMqttPublishReq.build(msg.getTopic(), MqttQoS.valueOf(msg.getQos()),
                    msg.getMessage(), msg.getMessageId(), msg.isDup());
                channel.writeAndFlush(req);
                TlRetryTask task = new TlRetryTask(messageId, msg, channel);
                retryManager.schedulePublishRetry(messageId, task);
                return Mono.empty();
            }), storeManager.getPubrelService().findAll(clientId)
            .doOnNext(msg -> log.debug("Resending PUBREL message 【{}】", msg.getMessageId())).flatMap(msg -> {
                TlMqttPubRelReq req = TlMqttPubRelReq.build(msg.getMessageId(), true);
                channel.writeAndFlush(req);
                TlRetryTask tlRetryTask = new TlRetryTask( msg.getMessageId(), msg, channel);
                retryManager.schedulePubrelRetry(msg.getMessageId(), tlRetryTask);
                return Mono.empty();
            })

        ).subscribe();
    }


}