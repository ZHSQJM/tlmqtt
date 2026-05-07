package com.tlmqtt.core.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tlmqtt.authentication.base.AuthenticationManager;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttDataSource;
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
import com.tlmqtt.common.rule.EventContext;
import com.tlmqtt.core.channel.TlChannelService;
import com.tlmqtt.core.service.ForwardMessageService;
import com.tlmqtt.core.task.TlSchedulerTaskService;
import com.tlmqtt.rule.engine.RuleEngineDispatcher;
import com.tlmqtt.store.service.PublishService;
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
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


/**
 * @author hszhou
 */
@Slf4j
@ChannelHandler.Sharable
public class TlConnectHandler extends AbstractTlHandler<TlMqttConnectReq> {



    private final TlSchedulerTaskService schedulerTaskService;
    private final  ForwardMessageService forwardMessageService;
    private final RuleEngineDispatcher ruleEngineDispatcher;

    public TlConnectHandler(SessionService sessionService, PublishService publishService,
        RetainService retainService, TlChannelService channelService, AuthenticationManager authenticationManager,
        MqttConfiguration mqttConfiguration,TlSchedulerTaskService schedulerTaskService,ForwardMessageService forwardMessageService,
        RuleEngineDispatcher ruleEngineDispatcher) {
        super.setSessionService(sessionService);
        super.setPublishService(publishService);
        super.setRetainService(retainService);
        super.setAuthenticationManager(authenticationManager);
        super.setChannelService(channelService);
        super.setMqttConfiguration(mqttConfiguration);
        this.schedulerTaskService = schedulerTaskService;
        this.forwardMessageService = forwardMessageService;
        this.ruleEngineDispatcher = ruleEngineDispatcher;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttConnectReq req, TlMqttSession session) {
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        MqttVersion version = MqttVersion.valueOf((byte) variableHead.getProtocolVersion());
        // 1. 解析ClientId
        String clientId = resolveClientId(req, version);
        log.debug("【TLMQTT】Handling 【CONNECT】 event from client:【{}】", clientId);
        // 2. 认证检查
        if (!authenticate(req)) {
            log.warn("【TLMQTT】 Authentication failed for client: [{}]", clientId);
            ctx.fireExceptionCaught(new TlAuthenticationException(true,MqttMessageType.CONNECT,MqttMessageType.CONNACK));
            return;
        }

        // 3. 开启响应式处理流水线
        processConnection(clientId,ctx, req, version)
            .doOnError(error -> log.error("【TLMQTT】Connection processing failed for [{}]: {}", clientId, error.getMessage()))
            .subscribe(); // 在Netty Handler中，这是链路的终点

        EventContext eventContext = EventContext.builder().dataSource(MqttDataSource.CLIENT_CONNECTED).clientId(clientId)
            .ip("127.0.0.1").timestamp(System.currentTimeMillis()).build();
        ruleEngineDispatcher.dispatch(eventContext);

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
            .flatMap(existingSession -> handleExistingSession(existingSession, cleanSession))
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
    private Mono<TlMqttSession> handleExistingSession(TlMqttSession oldSession , boolean cleanSession) {
        log.debug("【TLMQTT】Found existing session for client: [{}]", oldSession.getClientId());
        String clientId = oldSession.getClientId();
        Mono<Void> kickOutAction = Mono.empty();
        //如果上次的连接还存在并且还在连接 那么就把山谷的连接给关闭掉 保持唯一连接
        if (oldSession.getCtx() != null && oldSession.getCtx().channel().isActive()) {
            kickOutAction = closeChannel(oldSession);
        }

        if (cleanSession) {
            // 2. 如果是 CleanSession=1，清理所有存储并返回 Empty，触发 switchIfEmpty 创建全新实例
            return kickOutAction
                .then(sessionService.clearAll(clientId))
                .then(Mono.defer(() -> {

                    log.debug("【TLMQTT】Cancelling all tasks for client: [{}]", clientId);
                    // 也要取消所有可能的定时任务，防止清理后还有残留任务被触发
                    return cancelAllTasks(clientId);
                }))
                .then(Mono.empty());
        } else {
            // 3. 如果是 CleanSession=0，恢复旧会话
            log.debug("【TLMQTT】Restoring existing session for client: [{}]", clientId);
            return kickOutAction.then(cancelAllTasks(clientId)).thenReturn(oldSession.setFromStore(true));
        }
    }

    /**
     * 核心优化：统筹取消该客户端关联的所有调度任务
     */
    private Mono<Void> cancelAllTasks(String clientId) {
        // 使用 Flux.merge 确保并行触发，并收集结果
        return Flux.merge(
            schedulerTaskService.cancel(clientId + Constant.WILL)
                .doOnSuccess(v ->log.debug("【TLMQTT】Will task cancel: 【{}】", clientId)),
            schedulerTaskService.cancel(clientId + Constant.MQTT_SESSION)
                .doOnSuccess(v ->log.debug("【TLMQTT】Session task cancel 【{}】", clientId))
        ).then(); // 确保所有 cancel 执行完毕后再返回 Void
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
            .then(Mono.create(sink -> ctx.channel().writeAndFlush(connack).addListener(f -> {
                if (f.isSuccess()) {
                    sink.success();
                } else {
                    sink.error(f.cause());
                }
            })))
            // 3. 顺序处理后续业务逻辑
            .then(sessionService.save(session))
            .then(handleWillMessage(req))
            .then(handleRepublish(session, cleanSessionFromReq(req)))
            .doOnSuccess(v -> log.debug("【TLMQTT】 client [{}] connect success", session.getClientId()));
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
        if (cleanSession) {
            return Mono.empty();
        }
        String clientId = session.getClientId();
        // 使用 concatMap 确保前一个消息写完后再处理下一个，或者简单控制并发
        Flux<Void> pubFlow = publishService.findAll(clientId)
            .concatMap(pub -> republishSinglePublish(clientId, pub));

        Flux<Void> relFlow = publishService.findAllPubrel(clientId)
            .concatMap(rel -> republishSinglePubRel(clientId, rel));

        return Flux.concat(pubFlow, relFlow).then();


    }

    private Mono<Void> republishSinglePublish(String clientId, TlMqttPublishReq req) {
        long msgId = req.getVariableHead().getMessageId();

        // 1. 检查是否过期 (MQTT 5.0)
        if (isExpired(req)) {
            return publishService.clear(clientId, msgId).then();
        }

        // 3. 调用递归重试逻辑 (从第 1 次发送开始算起)
        // 这里直接复用之前在 ForwardMessageService 中定义的 scheduleWithRetry
        return forwardMessageService.scheduleWithRetry(Constant.PUBLISH, clientId, req, 1);
    }

    /**
     * 重发单个 PUBREL 报文 (QoS 2 第二阶段)
     */
    private Mono<Void> republishSinglePubRel(String clientId, TlMqttPubRelReq relReq) {
        return forwardMessageService.scheduleWithRetry(Constant.PUBREL, clientId, relReq, 1);
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
        channelService.put(session.getClientId(), channel);
    }

    private Mono<Void> closeChannel(TlMqttSession oldSession) {
        return Mono.create(sink -> {
            Channel channel = oldSession.getCtx().channel();
            if(oldSession.isVersion5()){
                TlMqttDisconnectReq disconnect = TlMqttDisconnectReq.build(MqttErrorCode.CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED);
                oldSession.getCtx().channel().writeAndFlush(disconnect)
                    .addListener(f -> sink.success());
            }else {
                channel.close().addListener(f -> sink.success());

            }
        });
    }

    private boolean isExpired(TlMqttPublishReq req) {
        if (req.getMqttVersion() != MqttVersion.MQTT_5) {
            return false;
        }
        Integer expiry = req.getVariableHead().getMessageExpiryInterval();
        if (expiry == null || req.getAcceptTime() == null) {
            return false;
        }
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
        log.debug("【TLMQTT】build will message");
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

        log.debug("【TLMQTT】save will message");
        return publishService.saveWill(clientId, req)
            .flatMap(saved -> retainAction);
    }
}