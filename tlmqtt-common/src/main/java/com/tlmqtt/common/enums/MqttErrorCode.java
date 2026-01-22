package com.tlmqtt.common.enums;

/**
 * mqtt的连接拒绝原因
 *
 * @author hszhou
 */
public enum MqttErrorCode {


    // 0x00 连接成功 CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, UNSUBACK, AUTH
    SUCCESS((byte)0x00),
    // 0x00 断开连接成功 DISCONNECT
    SUCCESS_DISCONNECTED((byte)0x00),
    //SUBACK
    SUCCESS_QOS0((byte) 0x00),
    //SUBACK
    SUCCESS_QOS1((byte)0x01),
    //SUBACK
    SUCCESS_QOS2((byte)0x02),
    //包含遗嘱的断开 DISCONNECT
    SUCCESS_DISCONNECTED_WILL((byte)0x04),
    //0x10	无匹配订阅	PUBACK, PUBREC
    //无匹配的订阅者（消息被接收，但是没有订阅者 只有服务端会发送次原因嘛 如果服务端得知没有匹配的订阅者 可以使用0x00（成功代替））--无用
    NO_MATCHING_SUBSCRIBERS((byte)0x10),
    //0x11	订阅不存在	UNSUBACK
    //订阅未发现
    NO_SUBSCRIPTION_EXISTED((byte)0x11),
    //0x18	继续认证	AUTH
    CONTINUE_AUTHENTICATION((byte)0x18),
    // 0x19	重新认证	AUTH
    RE_AUTHENTICATE((byte)0x19),
    //未指明的错误 (接收端不接受次消息 且不愿意透露错误原因或没有适应的原因码)--无用
    NO_MATCHING_SUBSCRIBERS_UNSPECIFIED_ERROR((byte)0x80),
    // 无效报文 CONNACK, DISCONNECT
    MALFORMED_MESSAGE((byte) 0x81),
    // 协议错误 	CONNACK, DISCONNECT
    PROTOCOL_ERROR((byte) 0x82),
    //实现特定错误(Publish报文有效 但是不被接收端所接受) --无用 CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT
    IMPLEMENTATION_SPECIFIC_ERROR((byte)0x83),
    // 5.0 服务端不支持客户端所请求的MQTT协议版本 CONNACK
    UNACCEPTABLE_PROTOCOL_VERSION((byte)0x84),
    //如果服务端拒绝了某个客户标识符（ClientID） CONNACK
    REFUSED_CLIENT_IDENTIFIER((byte)0x85),

    //4	0x86用户名密码错误 	CONNACK
    USERNAME_OR_PASSWORD_INVALID((byte)0x86),
    //未授权 CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT
    UNAUTHORIZED((byte)0x87),
    //0x88	服务端不可用	CONNACK
    SERVER_UNAVAILABLE((byte)0x88),
    //0x89	服务端正忙	CONNACK, DISCONNECT
    SERVER_BUSY((byte)0x89),
    //0x8A 禁止 CONNACK
    FORBIDDEN((byte)0x8A),
    //0x8B	服务端不可用	DISCONNECT
    SERVER_SHUTTING_DOWN((byte)0x8B),
    //0x8C 无效的认证方法 CONNACK, DISCONNECT
    INVALID_AUTHENTICATION_METHOD((byte)0x8C),
    // 0x8D 保活超时	DISCONNECT
    RETAIN_NOT_SUPPORTED((byte)0x8D),
    // 0x8E 会话被接管 发送disconnect 如果已经被接管了 DISCONNECT
    SESSION_TAKEN_OVER((byte)0x8E),
    //0x8F 主题过滤器无效 SUBACK, UNSUBACK, DISCONNECT 格式正切 语法合肥 但是服务端不接受
    TOPIC_FILTER_INVALID((byte)0x8F),
    //主题名无效 主题名格式正确 但未被客户端或服务端所接受 CONNACK, PUBACK, PUBREC, DISCONNECT
    TOPIC_NAME_INVALID((byte)0x90),
    // 报文标识符被占用 报文表示以被占用 肯呢个表明客户端和服务端之前的会话状态不匹配 PUBACK, PUBREC, SUBACK, UNSUBACK
    MESSAGE_IDENTIFIER_IN_USE((byte)0x91),
    //	0x92	报文标识符无效	PUBREL, PUBCOMP
    MESSAGE_IDENTIFIER_NOT_FOUND((byte)0x92),
    //	0x93	接收超出最大数量	DISCONNECT
    RECEIVE_MAXIMUM_EXCEEDED((byte)0x93),
    // 主题别名无效 CONNACK, PUBACK, PUBREC, DISCONNECT
    TOPIC_ALIAS_INVALID((byte)0x94),
    //0x95	报文过长	CONNACK, DISCONNECT
    CONNECTION_REFUSED_MESSAGE_TOO_LARGE((byte)0x95),
    //0x96	消息太过频繁	DISCONNECT
    PUBLISH_EVERY_SECOND((byte)0x96),
    //超出配合 已超出实现显示或管理限制 	CONNACK, PUBACK, PUBREC, SUBACK, DISCONNECT
    QUOTA_EXCEEDED((byte)0x97),
    //0x98	管理行为	DISCONNECT
    REQUEST_REJECTED((byte)0x98),
    //载荷格式无效
    CONNECTION_REFUSED_PAYLOAD_FORMAT_INVALID((byte)0x99),
    // 不支持保留
    CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED((byte)0x9A),
    // 不支持的qos等级
    CONNECTION_REFUSED_QOS_NOT_SUPPORTED((byte)0x9B),
    //0x9C	（临时）使用其他服务端	CONNACK, DISCONNECT
    CONNECTION_REFUSED_USE_ANOTHER_SERVICE_ENDPOINT((byte)0x9C),
    //0x9D	服务端已（永久）移动	CONNACK, DISCONNECT
    CONNECTION_REFUSED_SERVER_MOVED((byte)0x9D),
    //共享订阅不支持 SUBACK, DISCONNECT
    SHARED_SUBSCRIPTION_AVAILABLE((byte)0x9E),
    //0x9F	超出连接速率限制	CONNACK, DISCONNECT
    CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED((byte)0x9F),
    //	0xA0	最大连接时间	DISCONNECT
    CONNECTION_REFUSED_MAXIMUM_CONNECT_TIME((byte)0xA0),
    //订阅标识符不支持
    SUBSCRIPTION_IDENTIFIERS_AVAILABLE((byte)0xA1),
    //通配符订阅不支持
    WILDCARD_SUBSCRIPTION_AVAILABLE((byte)0xA2),




    ;




    private final byte byteValue;

     MqttErrorCode(byte byteValue) {
         this.byteValue = byteValue;
    }

    public byte byteValue() {
        return this.byteValue;
    }


}
