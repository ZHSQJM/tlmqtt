package com.tlmqtt.common.model.variable;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description:
 * mqtt的请求可变头
 * 协议名称 协议 连接标识 保持连接
 */
public class TlMqttSubAckVariableHead extends TlMqttSubscribeVariableHead {


    public TlMqttSubAckVariableHead(int messageId) {
        super(messageId);
    }

    public static TlMqttSubAckVariableHead of(int messageId){
        return new TlMqttSubAckVariableHead(messageId);
    }
}
