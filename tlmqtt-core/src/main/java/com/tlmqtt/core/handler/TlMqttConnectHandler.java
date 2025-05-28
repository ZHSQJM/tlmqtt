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
import com.tlmqtt.core.TlStoreManager;
import com.tlmqtt.core.channel.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:06
 * @Description: 连接处理器
 */
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Slf4j
public class TlMqttConnectHandler extends SimpleChannelInboundHandler<TlMqttConnectReq> {


    private final TlStoreManager tlStoreManager;

    private final AuthenticationManager authenticationManager;

    private final ChannelManager channelManager;



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlMqttConnectReq req) throws Exception {

        log.debug("in 【CONNECT】 handler");
        Channel channel = ctx.channel();
        TlMqttConnectVariableHead variableHead = req.getVariableHead();
        TlMqttConnectPayload payload = req.getPayload();
        Short protocolVersion = variableHead.getProtocolVersion();
        //是否清除会话
        int cleanSession = variableHead.getCleanSession();
        String clientId = payload.getClientId();
        //协议不通过
        if (!validateProtocolVersion(protocolVersion, cleanSession, channel)) {
            return;
        }
        //连接标识  如果连接标识不是0 则断开连接
        int reserved = variableHead.getReserved();
        if (!validateReserved(reserved, channel)) {
            log.error("client【{}】 reserved is error，close", clientId);
            return;
        }
        String username = payload.getUsername();
        String password = payload.getPassword();
        //认证
        boolean authentication = authentication(username, password);
        log.debug("username【{}】，password【{}】is validate[{}]",username,password,authentication);
        if(!authentication){
            TlMqttConnack res = TlMqttConnack.build(cleanSession, MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
            channel.writeAndFlush(res);
            return;
        }

        //将通道保存起来
        channelManager.put(clientId, channel);
        // 设置客户端标识以及正常断开连接的标识位为false 断开标志位设备false，表示没有走正常断开的逻辑 等到走正常关闭的逻辑时就可以设置为true
        channel.attr(AttributeKey.valueOf(Constant.CLIENT_ID)).set(clientId);
        channel.attr(AttributeKey.valueOf(Constant.DISCONNECT)).set(false);
        channel.attr(AttributeKey.valueOf(Constant.USERNAME)).set(username);
        channel.attr(AttributeKey.valueOf(Constant.IP)).set(null);

        boolean sessionPresent = cleanSession != 0;
        //如果是1的话 就重新清除之前的会话
        Mono<Void> clearOperation = Mono.empty();
        if (sessionPresent) {
            clearOperation= tlStoreManager.clearAll(clientId);
        }

        clearOperation.then( tlStoreManager.getSessionService().find(clientId))
                /*
                 * 如果CleanSession被设置为0
                 * 服务器必须根据当前的会话状态恢复与客户端的通信（客户端的唯一标识作为会话的标识）。
                 * 如果没有与客户端唯一标识相关的绘画，服务端必须创建一个新的会话。
                 * 客户端和服务端在断开连接后必须存储会话[MQTT-3.1.2-4]
                 */
            .switchIfEmpty(Mono.defer(() -> Mono.just(TlMqttSession.build(clientId, sessionPresent))))
            .flatMap(session -> {
                TlMqttConnack res = TlMqttConnack.build(cleanSession, MqttConnectReturnCode.CONNECTION_ACCEPTED);
                channelManager.writeAndFlush(clientId, res);
                if (!sessionPresent) {
                    republish(clientId);
                    session.setTopics(new HashSet<>());
                    session.setCleanSession(sessionPresent);
                }
                return tlStoreManager.getSessionService().save(session);
            })
            .subscribe(e -> {
                log.info("save client 【{}】 session 【{}】",clientId,e);
                    //添加心跳检测
                    Short heartbeat = variableHead.getKeepAlive();
                    ctx.pipeline().addLast(new IdleStateHandler(0, 0, heartbeat, TimeUnit.SECONDS));
                });


        //保存遗嘱消息
        if (variableHead.getWillFlag() == 1) {
            saveWillMessage(payload.getWillTopic(), MqttQoS.valueOf(variableHead.getWillQos()),
                    variableHead.getWillRetain() != 0, payload.getWillMessage(), clientId)
                    .subscribe();
        }

    }

    /**
     * @description: 存储遗嘱消息
     * @author: hszhou
     * @datetime: 2025-04-28 16:48:35
     * @param:
     * @param: willTopic
     * @param: qoS
     * @param: isRetain
     * @param: messageContent
     * @param: clientId
     * @return: Mono<Boolean>
     **/
    private Mono<Boolean> saveWillMessage(String willTopic, MqttQoS qoS, boolean isRetain, String content, String clientId) {
        PublishMessage message = new PublishMessage();
        message.setMessageId(0L);
        message.setQos(qoS.value());
        message.setTopic(willTopic);
        message.setMessage(content);
        message.setClientId(clientId);
        message.setRetain(isRetain);
        message.setDup(false);
        Mono<Boolean> savePublisher = tlStoreManager.getPublishService().saveWill(clientId, message);
        Mono<Boolean> retainPublisher = isRetain ? tlStoreManager.getRetainService().save(willTopic, message) : Mono.empty();
        return savePublisher.then(retainPublisher);
    }

    /**
     * @description: 校验连接标识
     * 服务端必须验证CONNECT控制包的预留字段是否为0，如果不为0断开与客户端的连接[MQTT-3.1.2-3].
     * @author: hszhou
     * @datetime: 2025-04-28 15:53:05
     * @param:
     * @param: reserved
     * @param: channel
     * @return: boolean
     **/
    private boolean validateReserved(int reserved, Channel channel) {
        if (reserved != 0) {
            channel.close();
            return false;
        }
        return true;
    }




    private boolean validateProtocolVersion(Short protocolVersion, int cleanSession, Channel channel) {
        if (!(protocolVersion.toString().equals(MqttVersion.MQTT_5.protocolLevel() + "") || protocolVersion.toString().equals(MqttVersion.MQTT_3_1_1.protocolLevel() + ""))) {
            TlMqttConnack res = TlMqttConnack.build(cleanSession, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            channel.writeAndFlush(res);
            return false;
        }
        return true;
    }

    /**
     * 客户端接入认证
     * @author hszhou
     * @datetime: 2025-05-10 16:21:33
     * @param username 用户名
     * @param password 密码
     * @return boolean
     **/
    private boolean authentication( String username, String password) {
        return authenticationManager.authenticate(username,password);
    }



    /**
     * @description: 上线时补发qos1与qos2的消息
     * @author: hszhou
     * @datetime: 2025-05-08 15:09:57
     * @param:
     * @param: clientId
     * @return: void
     **/
    private void republish(String  clientId) {
        //补发qos1与qos的2的消息 未回复的消息
        log.debug("cleanSession is 0 and republish message");
        tlStoreManager.getPublishService().findAll(clientId)
            .subscribe(m->{
                log.debug("resend republish publish messageId 【{}】 to client 【{}】",m.getMessageId(),clientId);
                TlMqttPublishReq message = TlMqttPublishReq.build(m.getTopic(),MqttQoS.valueOf(m.getQos()),m.getMessage(),m.getMessageId(),m.isDup());
                channelManager.writeAndFlush(clientId,message);
            });

        tlStoreManager.getPubrelService().findAll(clientId)
            .subscribe(m->{
                log.debug("resend republish pubrel messageId 【{}】 to client 【{}】",m.getMessageId(),clientId);
                TlMqttPubRelReq message = TlMqttPubRelReq.build(m.getMessageId(),true);
                channelManager.writeAndFlush(clientId,message);
            });
    }
}
