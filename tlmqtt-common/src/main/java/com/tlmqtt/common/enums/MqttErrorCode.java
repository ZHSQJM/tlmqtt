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
    //订阅标识符不支持
    SUBSCRIPTION_IDENTIFIERS_AVAILABLE((byte)0xA1),
    //共享订阅不支持
    SHARED_SUBSCRIPTION_AVAILABLE((byte)0x9E),

    //无匹配的订阅者（消息被接收，但是没有订阅者 只有服务端会发送次原因嘛 如果服务端得知没有匹配的订阅者 可以使用0x00（成功代替））--无用
    NO_MATCHING_SUBSCRIBERS((byte)0x10),
    //未指明的错误 (接收端不接受次消息 且不愿意透露错误原因或没有适应的原因码)--无用
    NO_MATCHING_SUBSCRIBERS_UNSPECIFIED_ERROR((byte)0x80),
    //实现特定错误(Publish报文有效 但是不被接收端所接受) --无用
    IMPLEMENTATION_SPECIFIC_ERROR((byte)0x83),
    //未授权
    UNAUTHORIZED((byte)0x87),
    //主题名无效 主题名格式正确 但未被客户端或服务端所接受
    TOPIC_NAME_INVALID((byte)0x90),
    // 报文标识符被占用 报文表示以被占用 肯呢个表明客户端和服务端之前的会话状态不匹配
    MESSAGE_IDENTIFIER_IN_USE((byte)0x91),
    //超出配合 已超出实现显示或管理限制
    QUOTA_EXCEEDED((byte)0x97),
    //主题过滤器无效 主题过滤器格式正确 但不被允许
    TOPIC_INVALIDE((byte)0x8F),
    //订阅未发现
    NO_SUBSCRIPTION_EXISTED((byte)0x11),

    ;




    private final byte byteValue;

    private MqttErrorCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    public byte byteValue() {
        return this.byteValue;
    }


}
