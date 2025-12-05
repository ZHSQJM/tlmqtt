package com.tlmqtt.common.enums;

/**
 * mqtt的连接拒绝原因
 *
 * @author hszhou
 */
public enum MqttErrorCode {


    // 0x00 连接成功
    SUCCESS((byte)0x00),
    // 0x00 断开连接成功
    SUCCESS_DISCONNECTED((byte)0x00),
    SUCCESS_QOS0((byte) 0x00),
    SUCCESS_QOS1((byte)0x01),
    SUCCESS_QOS2((byte)0x02),
    //包含遗嘱的断开
    SUCCESS_DISCONNECTED_WILL((byte)0x04),

    // 无效报文
    MALFORMED_MESSAGE((byte) 0x81),
    // 协议错误
    PROTOCOL_ERROR((byte) 0x82),

    // 主题别名无效
    TOPIC_ALIAS_INVALID((byte)0x94),

    // 5.0 服务端不支持客户端所请求的MQTT协议版本
    UNACCEPTABLE_PROTOCOL_VERSION((byte)0x84),
    //如果服务端拒绝了某个客户标识符（ClientID）
    REFUSED_CLIENT_IDENTIFIER((byte)0x85),

    //会话被接管 发送disconnect 如果已经被接管了
    CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED((byte)0x8E),

    // 不支持的qos等级
    CONNECTION_REFUSED_QOS_NOT_SUPPORTED((byte)0x9B),

    // 不支持保留
    CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED((byte)0x9A),

    //报文太大
    CONNECTION_REFUSED_MESSAGE_TOO_LARGE((byte)0x95),

    //载荷格式无效
    CONNECTION_REFUSED_PAYLOAD_FORMAT_INVALID((byte)0x99),

    //通配符订阅不支持
    WILDCARD_SUBSCRIPTION_AVAILABLE((byte)0xA2),
    //订阅表示不可以
    SUBSCRIPTION_IDENTIFIERS_AVAILABLE((byte)0xA1),
    //共享订阅不支持
    SHARED_SUBSCRIPTION_AVAILABLE((byte)0x9E),
    ;




    private final byte byteValue;

    private MqttErrorCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    public byte byteValue() {
        return this.byteValue;
    }


}
