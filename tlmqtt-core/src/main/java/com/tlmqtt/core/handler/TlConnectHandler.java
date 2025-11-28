package com.tlmqtt.core.handler;

import cn.hutool.core.util.IdUtil;
import com.tlmqtt.auth.AuthenticationManager;
import com.tlmqtt.common.Constant;
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
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import com.tlmqtt.common.model.variable.TlMqttPubRelVariableHead;
import com.tlmqtt.common.model.variable.TlMqttPublishVariableHead;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.manager.MessageManager;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.task.TlRetryTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hszhou
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class TlConnectHandler extends AbstractTlHandler<TlMqttConnectReq>{

    private final TlStoreManager storeManager;

    private final ChannelManager channelManager;

    private final AuthenticationManager authenticationManager;

    private final RetryManager retryManager;

    private final MessageManager messageManager;

    @Override
    public void handle(ChannelHandlerContext ctx, TlMqttConnectReq req, TlMqttSession session) {
        Channel channel = ctx.channel();
        log.debug("Handling 【CONNECT】 event from client:【{}】", req.getPayload().getClientId());
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        short protocolVersion = variableHead.getProtocolVersion();
        MqttVersion mqttVersion = MqttVersion.valueOf((byte) protocolVersion);
        if(session != null){
            log.warn("Client:【{}】 has already connected", req.getPayload().getClientId());
            //在一个网络连接上，客户端只能发送一次CONNECT报文。服务端必须将客户端发送的第二个CONNECT报文当作协议违规处理并断开客户端的连接 [MQTT-3.1.0-2]。有关错误处理的信息请查看4.13节
            channel.close();
            return;
        }

        if (!authenticate(req)) {
            log.error("Authentication failed for client:【{}】", req.getPayload().getClientId());
            throw new TlAuthenticationException(mqttVersion);
        }

        handlerSession(req, ctx,mqttVersion)
            .then(handleWillMessage(req))
            .doOnSuccess(e->{
                //log.info("1保存will消息[{}]",e);
            })
            .subscribe(e->{
               // log.info("2保存will消息[{}]",e);
            });
    }

    /**
     * 用户名密码校验
     *
     * @param req 连接
     * @return boolean  是否认证成功

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
     * @return Mono<Boolean> 创建会话结果

     **/
    private Mono<Boolean> handlerSession(TlMqttConnectReq req, ChannelHandlerContext ctx,  MqttVersion mqttVersion ) {

        String clientId = req.getPayload().getClientId() != null ? req.getPayload().getClientId() :
            (mqttVersion == MqttVersion.MQTT_5 ? IdUtil.nanoId(16) : null);
        req.getPayload().setClientId(clientId);
        final String username = req.getPayload().getUsername();
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        boolean cleanSession = variableHead.getCleanSession() != 0;
        //是否存在会话 默认存在
        AtomicBoolean existSession = new AtomicBoolean(true);
        //如果cleanSession是1的话 就说明不保存会话 先把之前的会话清除掉 如果cleanSession是0的话 就不清除
        //查找clientId的会话 如果没有的话 就创建一个新的会话
        return Mono.defer(() -> cleanSession ? storeManager.clearAll(clientId) : Mono.empty())
            .then(storeManager.getSessionService().find(clientId))
            .doOnNext(session -> {
                log.info("Existing session found for clientId: {}，【{}】", clientId,session);
                // 走到这里之前存在会话  然后连接了 那么就将移除会话和发送遗嘱消息的定时任务取消掉
                storeManager.cancelRemoveSession(clientId);
            })
            .switchIfEmpty(Mono.defer(() ->{
                existSession.set(false);
                return createNewSession(clientId);
            }))
            .flatMap(session -> completeSessionHandling(session, req, ctx, cleanSession,mqttVersion, existSession.get(),username))
            .doOnSuccess(e -> setupHeartBeat(ctx, req.getVariableHead().getKeepAlive()));
    }

    /**
     * 会话处理完成
     * @author hszhou
     * @datetime: 2025-07-30 14:17:13
     * @param session 会话 比较干净或者旧的
     * @param req 连接的消息
     * @param ctx 通道
     * @param cleanSession 是否清除会话
     * @param mqttVersion 版本
     * @param existsSession 之前是否存在该会话
     * @param username 用户名
     * @return Mono<Boolean>
     **/
    private Mono<Boolean> completeSessionHandling(TlMqttSession session, TlMqttConnectReq req,
        ChannelHandlerContext ctx, boolean cleanSession,MqttVersion mqttVersion,boolean existsSession,String username) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String hostAddress = remoteAddress.getAddress().getHostAddress();
        String clientId = session.getClientId();
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        session.setKeepAlive(variableHead.getKeepAlive())
               .setCleanSession(cleanSession)
               .setIp(hostAddress)
               .setUsername(username)
               .setMqttVersion(mqttVersion)
               .setCtx(ctx);

        TlMqttConnackAck connackResponse = TlMqttConnackAck.build(variableHead.getCleanSession(), existsSession, MqttErrorCode.CONNECTION_ACCEPTED,mqttVersion,req.getPayload().getClientId(),variableHead.getKeepAlive());
        if(mqttVersion == MqttVersion.MQTT_5){
            //填充session的属性
            fillSession(session, variableHead);
        }


        registerClient(ctx.channel(),session);
        ctx.channel().writeAndFlush(connackResponse).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Connect success, clientId:【{}】", clientId);
            } else {
                log.error("Connect failed, clientId:【{}】", clientId);
            }
        });

        // 如果此次的cleanSession是false 则重新发布所有未确认的消息
        if (!cleanSession) {
            republish(clientId, ctx.channel(),mqttVersion);
        }
        messageManager.cancelSendWillMessage(clientId);
        return storeManager.getSessionService().save(session);
    }


    /**
     * 填充session的属性
     *
     * @param session 会话
     * @param variableHead 变量头
     **/
    private  void fillSession(TlMqttSession session, TlMqttConnectVariableHead variableHead) {
        session.setReceiveMaximum(variableHead.getReceiveMaximum()==null?Short.MAX_VALUE: variableHead.getReceiveMaximum())
               .setMaximumPacketSize(variableHead.getMaximumPacketSize())
               .setTopicMaxAlias( variableHead.getTopicMaxAlias())
               .setRequestProblemInformation(variableHead.isRequestProblemInformation())
               .setSessionExpiryInterval(variableHead.getSessionExpiryInterval())
               .setUserProperties(variableHead.getUserProperty())
               .setRequestResponseInformation(variableHead.isRequestResponseInformation());
        //log.info("[{}]",variableHead.getMaximumPacketSize());
    }

    /**
     * 创建新的会话
     * @param clientId 客户端
     * @return Mono<TlMqttSession> 新会话
     **/
    private Mono<TlMqttSession> createNewSession(String clientId) {
        TlMqttSession session = TlMqttSession
            .builder()
            //客户端ID
            .clientId(clientId)
            //订阅的主题集合
            .topics(new HashSet<>())
            .build();
        return Mono.just(session);
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
                                                                             .topic(payload.getWillTopic())
                                                                             .payloadFormatIndicator(payload.getPayloadFormatIndicator())
                                                                             .messageExpiryInterval(payload.getMessageExpiryInterval())
                                                                             .responseTopic(payload.getResponseTopic())
                                                                             .correlationData(payload.getCorrelationData())
                                                                             .userProperties(payload.getUserProperty())
                                                                             .contentType(payload.getContentType())
            .willDelayInterval(payload.getWillDelayInterval())
                                                                             .build();

        TlMqttPublishPayload pubPayload = TlMqttPublishPayload.builder()
                                                              .content(payload.getWillMessage())
                                                              .build();
        TlMqttFixedHead fixedHead = TlMqttFixedHead.builder()
            .messageType(MqttMessageType.PUBLISH)
            .qos(mqttQoS)
            .retain(variableHead.getWillRetain() == 1)
            .build();
        TlMqttPublishReq publishReq =TlMqttPublishReq.build(fixedHead,pubVariableHead,pubPayload,MqttVersion.MQTT_5) ;
        return saveWillMessage(publishReq, payload.getClientId(), variableHead.getWillRetain()==1, payload.getWillTopic());
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
    private Mono<Boolean> saveWillMessage(TlMqttPublishReq req,String clientId,boolean isRetain,String willTopic) {

        Mono<Boolean> retainAction = isRetain
            ? storeManager.getRetainService().save(willTopic, req)
            : Mono.empty();

        return storeManager.getPublishService()
            .saveWill(clientId, req)
            .flatMap(retainAction::thenReturn);
    }


    /**
     * 注册客户端信息到channel中
     *
     * @param session 会话
     * @param channel 通道
     **/
    private  void registerClient(Channel channel,TlMqttSession session) {
        // 先设置channel属性
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);
        channel.attr(AttributeKey.valueOf(Constant.MQTT_SESSION)).set(session);
        // 注册到ChannelManager（会处理旧连接）
        channelManager.put(session.getClientId(), channel);

    }

    /**
     * 重发消息
     *
     * @param clientId 客户端ID
     **/
    private void republish(String clientId, Channel channel,MqttVersion mqttVersion) {

        Flux.merge(storeManager.getPublishService()
                               .findAll(clientId)
                               .flatMap(publishReq -> {

//                                   TlMqttFixedHead fixedHead = publishReq.getFixedHead();
//                                   TlMqttPublishReq req = messageManager.build(publishReq, fixedHead.getQos(),
//                                       mqttVersion);
                                  TlMqttPublishVariableHead variableHead = publishReq.getVariableHead();
//                                   log.debug("Resending PUBLISH messageIs 【{}】", variableHead.getMessageId());
                                   Long messageId = variableHead.getMessageId();
                                   //如果消息是mqtt5的话 需要判断国企时间
                                   if(publishReq.getMqttVersion() == MqttVersion.MQTT_5){
                                       log.info("ddd");
                                       Integer messageExpiryInterval = variableHead.getMessageExpiryInterval();

                                       Long acceptTime = publishReq.getAcceptTime();
                                       log.info("【{}】-【{}】",messageExpiryInterval,acceptTime);
                                       if(messageExpiryInterval !=null && acceptTime !=null){
                                           long now = System.currentTimeMillis() / 1000;
                                           //表示过期了
                                           if(now>messageExpiryInterval+acceptTime){
                                               return Mono.empty();
                                           }
                                           int remainding =(int)(messageExpiryInterval-(now-acceptTime));
                                           log.info("转到5的客户端当前时间。【{}}剩余时间[{}]，国企时间【{}}",now,remainding,messageExpiryInterval);
                                           variableHead.setMessageExpiryInterval(remainding);
                                       }
                                   }
                                   channel.writeAndFlush(publishReq).addListener(future -> {
                                        storeManager.getPublishService().save(clientId,messageId,publishReq)
                                            .subscribe(da->{
                                               TlRetryTask task = new TlRetryTask(messageId, publishReq, channel);
                                                retryManager.schedulePublishRetry(messageId, task);
                                            });
                                    });

                                    return Mono.empty();
                               }),
                   storeManager.getPubrelService().
                                findAll(clientId)
                                .flatMap(pubrelReq -> {
                                    TlMqttPubRelVariableHead variableHead = pubrelReq.getVariableHead();
                                    log.debug("Resending PUBREL message 【{}】", variableHead.getMessageId());
                                    channel.writeAndFlush(pubrelReq);
                                    TlRetryTask tlRetryTask = new TlRetryTask( variableHead.getMessageId(), pubrelReq, channel);
                                    retryManager.schedulePubrelRetry(variableHead.getMessageId(), tlRetryTask);
                                    return Mono.empty();
                                  })

        ).subscribe();
    }


}