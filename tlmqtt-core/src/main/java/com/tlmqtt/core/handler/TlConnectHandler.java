package com.tlmqtt.core.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.exception.*;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.payload.TlMqttPublishPayload;
import com.tlmqtt.common.model.request.TlMqttConnectReq;
import com.tlmqtt.common.model.request.TlMqttDisconnectReq;
import com.tlmqtt.common.model.request.TlMqttPubRelReq;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.task.TlRetryTask;
import com.tlmqtt.store.service.PublishService;
import com.tlmqtt.store.service.PubrelService;
import com.tlmqtt.store.service.RetainService;
import com.tlmqtt.store.service.session.SessionService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlConnectHandler extends AbstractTlHandler<TlMqttConnectReq> {



    public TlConnectHandler(SessionService sessionService, PublishService publishService, PubrelService pubrelService,
        RetainService retainService, ChannelManager channelManager, AuthenticationManager authenticationManager,
        RetryManager retryManager, MqttConfiguration mqttConfiguration) {
        super.setSessionService(sessionService);
        super.setPublishService(publishService);
        super.setPubrelService(pubrelService);
        super.setRetainService(retainService);
        super.setAuthenticationManager(authenticationManager);
        super.setRetryManager(retryManager);
        super.setChannelManager(channelManager);
        super.setMqttConfiguration(mqttConfiguration);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttConnectReq req, TlMqttSession session) {
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        MqttVersion version = MqttVersion.valueOf((byte) variableHead.getProtocolVersion());
        // 1. 解析ClientId
        String clientId = resolveClientId(req, version);
        // 1. 认证检查
        if (!authenticate(req)) {
           log.warn("Authentication failed for client: [{}]", clientId);
            ctx.fireExceptionCaught(new TlAuthenticationException(MqttMessageType.CONNACK));
            return;
        }

        // 2. 开启响应式处理流水线
        processConnection(clientId,ctx, req, version)
            //// 防止数据库挂起导致连接卡死
            .timeout(Duration.ofSeconds(30))
            .doOnError(error -> log.error("Connection processing failed for [{}]: {}", clientId, error.getMessage()))
            .subscribe(); // 在Netty Handler中，这是链路的终点
    }

    /**
     * 用户名密码校验
     * @param req 连接
     * @return boolean  是否认证成功
     **/
    private boolean authenticate(TlMqttConnectReq req) {
        TlMqttConnectPayload payload = req.getPayload();
        return authenticationManager.authenticate(payload.getUsername(), payload.getPassword());
    }

    /**
     * 核心处理流程
     * @author zhouhs
     * @param: clientId 客户端ID
     * @param: ctx 通道
     * @param: req 连接请求
     * @param: version  版本
     * @return: reactor.core.publisher.Mono<java.lang.Void>
     **/

    private Mono<Void> processConnection(String clientId,ChannelHandlerContext ctx, TlMqttConnectReq req, MqttVersion version) {

        //是否是清除会话 true 表示是 false表示不是
        boolean cleanSession = req.getVariableHead().getCleanSession() != 0;
        return sessionService.find(clientId)
            // 1. 获取已存在的会话
            .flatMap(existingSession -> handleExistingSession(existingSession, version, cleanSession))
            .switchIfEmpty(createNewSession(clientId))
            .flatMap(session -> {
                // 更新会话状态与属性
                updateSessionInfo(session, req, ctx, version, cleanSession);
                // 判断 Session Present (协议核心：根据是否是从持久化库中恢复且cleanSession=false)
                boolean sessionPresent = !cleanSession && session.isFromStore();
                return saveAndResponse(ctx, req, session, sessionPresent);
            });
    }


    /**
     * 处理已存在的会话
     * @author zhouhs
     * @param: oldSession 旧会话
     * @param: version  版本
     * @param: cleanSession  是否清理会话
     * @return: reactor.core.publisher.Mono<com.tlmqtt.common.model.TlMqttSession>
     **/
    private Mono<TlMqttSession> handleExistingSession(TlMqttSession oldSession, MqttVersion version, boolean cleanSession) {
        String clientId = oldSession.getClientId();
        Mono<Void> kickOutAction = Mono.empty();
        //如果上次的连接还存在并且还在连接 那么就把山谷的连接给关闭掉 保持唯一连接
        if (oldSession.getCtx() != null && oldSession.getCtx().channel().isActive()) {
            log.info("Client [{}] conflict. Kicking out old connection.", clientId);
            MqttErrorCode code = (version == MqttVersion.MQTT_5)
                ? MqttErrorCode.CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED
                : MqttErrorCode.SUCCESS;
            kickOutAction = closeChannel(oldSession.getCtx().channel(), code);
        }
        // 协议要求：如果是 cleanSession，则清除旧状态
        Mono<Void> clearAction = cleanSession ? sessionService.clearAll(clientId).then() : Mono.empty();
        return kickOutAction.then(clearAction).then(sessionService.cancelRemoveSession(clientId))
            //// 标记为从存储恢复
            .thenReturn(oldSession.setFromStore(true));
    }


    private Mono<Void> saveAndResponse(ChannelHandlerContext ctx, TlMqttConnectReq req, TlMqttSession session, boolean sessionPresent) {
        MqttVersion version = session.getMqttVersion();
        TlMqttConnackAck connack = TlMqttConnackAck.build(sessionPresent ? 1 : 0, MqttErrorCode.SUCCESS, version,
            session.getClientId(), session.getKeepAlive(),mqttConfiguration);

        // 1. 将同步操作封装进 Runnable
        return Mono.fromRunnable(() -> {
                registerToChannel(ctx.channel(), session);
                setupHeartBeat(ctx, session.getKeepAlive());
            })
            // 2. 发送响应报文
            .then(Mono.create(sink -> {
                ctx.channel().writeAndFlush(connack).addListener(f -> {
                    if (f.isSuccess()) sink.success();
                    else sink.error(f.cause());
                });
            }))
            // 3. 顺序处理后续业务逻辑
            .then(sessionService.save(session))
            .then(handleWillMessage(req))
            .then(handleRepublish(session, cleanSessionFromReq(req)))
            .doOnSuccess(v -> log.info("Client [{}] fully initialized.", session.getClientId()));
    }

    //offlineMessageService.triggerRedelivery(session, ctx.channel());

    /**
     * 重新发送qos1与qos2消息
     * @author zhouhs
     * @param: session
     * @param: cleanSession
     * @return: reactor.core.publisher.Mono<java.lang.Void>
     **/

    private Mono<Void> handleRepublish(TlMqttSession session, boolean cleanSession) {
        // 如果是清理会话，则不重新发送
        if (cleanSession) return Mono.empty();
        String clientId = session.getClientId();
        Channel channel = session.getCtx().channel();

        // 使用 concatMap 确保前一个消息写完后再处理下一个，或者简单控制并发
        Flux<Void> pubFlow = publishService.findAll(clientId)
            .concatMap(pub -> republishSinglePublish(clientId, channel, pub));

        Flux<Void> relFlow = pubrelService.findAll(clientId)
            .concatMap(rel -> republishSinglePubRel(clientId, channel, rel));

        return Flux.concat(pubFlow, relFlow).then();
    }

    private Mono<Void> republishSinglePublish(String clientId, Channel channel, TlMqttPublishReq req) {
        if (isExpired(req)) {
            return publishService.clear(clientId, req.getVariableHead().getMessageId()).then();
        }
        return Mono.fromRunnable(() -> {
            channel.writeAndFlush(req);
            long msgId = req.getVariableHead().getMessageId();
            retryManager.schedulePublishRetry(msgId, new TlRetryTask(msgId, req, channel));
        });
    }

    /**
     * 重发单个 PUBREL 报文 (QoS 2 第二阶段)
     */
    private Mono<Void> republishSinglePubRel(String clientId, Channel channel, TlMqttPubRelReq relReq) {
        return Mono.fromRunnable(() -> {
            long messageId = relReq.getVariableHead().getMessageId();

            log.debug("Republishing PUBREL for client: [{}], messageId: [{}]", clientId, messageId);

            // 1. 发送 PUBREL 报文
            channel.writeAndFlush(relReq).addListener(future -> {
                if (future.isSuccess()) {
                    // 2. 注册重试任务
                    // 注意：TlRetryTask 需要能识别不同的消息类型（PUBLISH 或 PUBREL）
                    TlRetryTask retryTask = new TlRetryTask(messageId, relReq, channel);
                    retryManager.schedulePubrelRetry(messageId, retryTask);
                } else {
                    log.error("Failed to resend PUBREL to client [{}], msgId: [{}]", clientId, messageId, future.cause());
                }
            });
        });
    }

    /**
     * 解析clientId 如果为空并且是v5则生成一个
     * @author zhouhs
     * @param: req
     * @param: version
     * @return: java.lang.String
     *
     **/
    private String resolveClientId(TlMqttConnectReq req, MqttVersion version) {
        String clientId = req.getPayload().getClientId();
        if (StrUtil.isEmpty(clientId) && version == MqttVersion.MQTT_5) {
            return IdUtil.nanoId(12);
        }
        return clientId;
    }

        private Mono<TlMqttSession> createNewSession(String clientId) {
            return Mono.just(
                TlMqttSession.builder()
                    .clientId(clientId)
                    .topics(new HashSet<>())
                    .fromStore(false)
                    .build());
        }

        private void registerToChannel(Channel channel, TlMqttSession session) {
            channel.attr(Constant.DISCONNECT_KEY).set(false);
            channel.attr(Constant.SESSION_KEY).set(session);
            channelManager.put(session.getClientId(), channel);
        }

        private Mono<Void> closeChannel(Channel channel, MqttErrorCode code) {
            return Mono.create(sink -> {
                TlMqttDisconnectReq disconnect = TlMqttDisconnectReq.build(code);
                channel.writeAndFlush(disconnect).addListener(f -> {
                    channel.close();
                    sink.success();
                });
            });
        }

        private boolean isExpired(TlMqttPublishReq req) {
            if (req.getMqttVersion() != MqttVersion.MQTT_5)
                return false;
            Integer expiry = req.getVariableHead().getMessageExpiryInterval();
            if (expiry == null || req.getAcceptTime() == null)
                return false;
            return (System.currentTimeMillis() / 1000) > (req.getAcceptTime() + expiry);
        }

        private boolean cleanSessionFromReq(TlMqttConnectReq req) {
            return req.getVariableHead().getCleanSession() != 0;
        }

        private void updateSessionInfo(TlMqttSession session, TlMqttConnectReq req, ChannelHandlerContext ctx,
            MqttVersion version, boolean cleanSession) {
            InetSocketAddress adder = (InetSocketAddress) ctx.channel().remoteAddress();
            TlMqttConnectVariableHead vHead = req.getVariableHead();

            session.setMqttVersion(version)
                .setCleanSession(cleanSession)
                .setKeepAlive(vHead.getKeepAlive())
                .setIp(adder.getAddress().getHostAddress())
                .setCtx(ctx)
                .setUsername(req.getPayload().getUsername());
            if (version == MqttVersion.MQTT_5) {
                fillSession(session, vHead);
            }
        }




        /**
         * 填充session的属性
         *
         * @param session 会话
         * @param variableHead 变量头
         **/
        private void fillSession(TlMqttSession session, TlMqttConnectVariableHead variableHead) {
            session.setReceiveMaximum(
                    variableHead.getReceiveMaximum() == null ? Short.MAX_VALUE : variableHead.getReceiveMaximum())
                .setMaximumPacketSize(variableHead.getMaximumPacketSize())
                .setTopicMaxAlias(variableHead.getTopicMaxAlias())
                .setRequestProblemInformation(variableHead.isRequestProblemInformation())
                .setSessionExpiryInterval(variableHead.getSessionExpiryInterval())
                .setUserProperties(variableHead.getUserProperty())
                .setRequestResponseInformation(variableHead.isRequestResponseInformation());
        }

        /**
         * 添加心跳
         *
         * @param ctx 通道
         * @param keepAlive 心跳间隔
         **/
        private void setupHeartBeat(ChannelHandlerContext ctx, short keepAlive) {
            //todo 如果保持连接的值非零，并且服务端在1.5倍的保持连接时间内没有收到客户端的控制报文，它必须断开客户端的网络连接，并判定网络连接已断开 [MQTT-3.1.2-22]。 mqtt5
            ctx.pipeline().addLast(new IdleStateHandler(0, 0, keepAlive, TimeUnit.SECONDS));
        }

        /**
         * 处理遗嘱消息
         *
         * @param req 连接报文
         * @return Mono<Void> 处理结果
         **/
        private Mono<Boolean> handleWillMessage(TlMqttConnectReq req) {
            TlMqttConnectVariableHead variableHead = req.getVariableHead();
            if (variableHead.getWillFlag() != 1) {
                return Mono.empty();
            }
            TlMqttConnectPayload payload = req.getPayload();
            MqttQoS mqttQoS = MqttQoS.valueOf(variableHead.getWillQos());

            TlMqttPublishVariableHead pubVariableHead = TlMqttPublishVariableHead.builder()
                .topic(payload.getWillTopic()).payloadFormatIndicator(payload.getPayloadFormatIndicator())
                .messageExpiryInterval(payload.getMessageExpiryInterval()).responseTopic(payload.getResponseTopic())
                .correlationData(payload.getCorrelationData()).userProperties(payload.getUserProperty())
                .contentType(payload.getContentType()).willDelayInterval(payload.getWillDelayInterval()).build();

            TlMqttPublishPayload pubPayload = TlMqttPublishPayload.builder().content(payload.getWillMessage()).build();
            TlMqttFixedHead fixedHead = TlMqttFixedHead.builder().messageType(MqttMessageType.PUBLISH).qos(mqttQoS)
                .retain(variableHead.getWillRetain() == 1).build();
            TlMqttPublishReq publishReq = TlMqttPublishReq.build(fixedHead, pubVariableHead, pubPayload,
                MqttVersion.MQTT_5);
            return saveWillMessage(publishReq, payload.getClientId(), variableHead.getWillRetain() == 1,
                payload.getWillTopic());
        }

        /**
         * 存储遗嘱消息
         *
         * @param req 遗嘱消息主题
         * @param clientId 客户端ID
         * @param isRetain 是否是保留消息
         * @param willTopic 遗嘱消息内容
         * @return Mono<Boolean> 保存结果
         **/
    private Mono<Boolean> saveWillMessage(TlMqttPublishReq req, String clientId, boolean isRetain, String willTopic) {
        // 即使不存 retain，也要确保流继续向下传递 true
        Mono<Boolean> retainAction = isRetain
            ? retainService.save(willTopic, req)
            : Mono.just(true);

        return publishService.saveWill(clientId, req)
            .flatMap(saved -> retainAction);
    }
}