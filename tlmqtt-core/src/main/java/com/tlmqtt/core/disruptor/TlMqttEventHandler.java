//package com.tlmqtt.core.disruptor;
//
//import com.lmax.disruptor.WorkHandler;
//import com.tlmqtt.common.enums.MqttMessageType;
//import com.tlmqtt.common.model.request.*;
//import com.tlmqtt.core.handler.*;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class TlMqttEventHandler implements WorkHandler<TlMqttEvent> {
//
//    @Override
//    public void onEvent(TlMqttEvent event) throws Exception {
//        try {
//            // 根据消息类型调用相应的handler
//            AbstractTlMessage message = event.getMessage();
//            MqttMessageType messageType = message.getMessageType();
//
//            log.info("处理MQTT消息事件: {}", message);
//
//            switch (messageType) {
//                case CONNECT:
//                    // 连接消息由TlConnectHandler处理
//                    TlConnectHandler connectHandler = new TlConnectHandler();
//                    connectHandler.handle(event.getCtx(), (TlConnectMessage) message, event.getSession());
//                    break;
//                case PUBLISH:
//                    // 发布消息由TlPublishHandler处理
//                    TlPublishHandler publishHandler = new TlPublishHandler();
//                    publishHandler.handle(event.getCtx(), (TlPublishMessage) message, event.getSession());
//                    break;
//                case PUBACK:
//                    // PUBACK消息由TlPubAckHandler处理
//                    TlPubAckHandler pubAckHandler = new TlPubAckHandler();
//                    pubAckHandler.handle(event.getCtx(), (TlPubAckMessage) message, event.getSession());
//                    break;
//                case PUBREC:
//                    // PUBREC消息由TlPubRecHandler处理
//                    TlPubRecHandler pubRecHandler = new TlPubRecHandler();
//                    pubRecHandler.handle(event.getCtx(), (TlPubRecMessage) message, event.getSession());
//                    break;
//                case PUBREL:
//                    // PUBREL消息由TlPubRelHandler处理
//                    TlPubRelHandler pubRelHandler = new TlPubRelHandler();
//                    pubRelHandler.handle(event.getCtx(), (TlPubRelMessage) message, event.getSession());
//                    break;
//                case PUBCOMP:
//                    // PUBCOMP消息由TlPubCompHandler处理
//                    TlPubCompHandler pubCompHandler = new TlPubCompHandler();
//                    pubCompHandler.handle(event.getCtx(), (TlPubCompMessage) message, event.getSession());
//                    break;
//                case SUBSCRIBE:
//                    // 订阅消息由TlSubscribeHandler处理
//                    TlSubscribeHandler subscribeHandler = new TlSubscribeHandler();
//                    subscribeHandler.handle(event.getCtx(), (TlSubscribeMessage) message, event.getSession());
//                    break;
//                case UNSUBSCRIBE:
//                    // 取消订阅消息由TlUnSubscribeHandler处理
//                    TlUnSubscribeHandler unSubscribeHandler = new TlUnSubscribeHandler();
//                    unSubscribeHandler.handle(event.getCtx(), (TlUnSubscribeMessage) message, event.getSession());
//                    break;
//                case PINGREQ:
//                    // 心跳消息由TlHeartBeatHandler处理
//                    TlHeartBeatHandler heartBeatHandler = new TlHeartBeatHandler();
//                    heartBeatHandler.handle(event.getCtx(), (TlHeartBeatMessage) message, event.getSession());
//                    break;
//                case DISCONNECT:
//                    // 断开连接消息由TlDisconnectHandler处理
//                    TlDisconnectHandler disconnectHandler = new TlDisconnectHandler();
//                    disconnectHandler.handle(event.getCtx(), (TlDisconnectMessage) message, event.getSession());
//                    break;
//                default:
//                    log.warn("未知的消息类型: {}", messageType);
//                    break;
//            }
//        } catch (Exception e) {
//            log.error("处理MQTT消息事件时发生错误: ", e);
//        }
//    }
//}