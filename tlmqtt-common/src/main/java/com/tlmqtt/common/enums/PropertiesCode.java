package com.tlmqtt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用于表示属性的标识符
 * @author hszhou
 */
@Getter
@AllArgsConstructor
public enum PropertiesCode {


    /**
     * 0x01	载荷格式说明	字节	PUBLISH, Will Properties
     */
    PAYLOAD_FORMAT_INDICATOR((byte) 0x01),
    /**
     * 0x02	消息过期时间	四字节整数	PUBLISH, Will Properties
     */
    MESSAGE_EXPIRY_INTERVAL((byte) 0x02),
    /**
     * 0x03	内容类型	UTF-8编码字符串	PUBLISH, Will Properties
     */
    CONTENT_TYPE((byte) 0x03),
    /**
     * 0x08	响应主题	UTF-8编码字符串	PUBLISH, Will Properties
     */
    RESPONSE_TOPIC((byte) 0x08),
    /**
     * 0x09	关联数据	二进制数据	PUBLISH, Will Properties
     */
    CORRELATION_DATA((byte) 0x09),
    /**
     * 0x0B	定义标识符	变长字节整数	PUBLISH, SUBSCRIBE
     */
    SUBSCRIPTION_IDENTIFIER((byte) 0x0B),
    /**
     * 0x11	会话过期间隔	四字节整数	CONNECT, CONNACK, DISCONNECT
     */
    SESSION_EXPIRY_INTERVAL((byte) 0x11),
    /**
     * 0x12	分配客户标识符	UTF-8编码字符串	CONNACK
     */
    ASSIGNED_CLIENT_IDENTIFIER((byte) 0x12),
    /**
     * 0x13	服务端保活时间	双字节整数	CONNACK
     */
    SERVER_KEEP_ALIVE((byte) 0x13),
    /**
     * 0x15	认证方法	UTF-8编码字符串	CONNECT, CONNACK, AUTH
     */
    AUTHENTICATION_METHOD((byte) 0x15),
    /**
     * 0x16	认证数据	二进制数据	CONNECT, CONNACK, AUTH
     */
    AUTHENTICATION_DATA((byte) 0x16),
    /**
     * 0x17	请求问题信息	字节	CONNECT
     */
    REQUEST_PROBLEM_INFORMATION((byte)0x17),
    /**
     * 0x18	遗嘱延时间隔	四字节整数	Will Properties
     */
    WILL_DELAY_INTERVAL((byte)0x18),
    /**
     * 0x19	请求响应信息	字节	CONNECT
     */
    REQUEST_RESPONSE_INFORMATION((byte)0x19),
    /**
     * 0x1A	请求信息	UTF-8编码字符串	CONNACK
     */
    REQUEST_INFORMATION((byte)0x1A),
    /**
     * 0x1C	服务端参考	UTF-8编码字符串	CONNACK, DISCONNECT
     */
    SERVER_REFERENCE((byte)0x1C),
    /**
     * 0x1F	原因字符串	UTF-8编码字符串	CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBACK, UNSUBACK, DISCONNECT, AUTH
     */
    REASON_STRING((byte)0x1F),
    /**
     * 0x21	接收最大数量	双字节整数	CONNECT, CONNACK
     */
    RECEIVE_MAXIMUM((byte)0x21),
    /**
     * 0x22	主题别名最大长度	双字节整数	CONNECT, CONNACK
     */
    TOPIC_ALIAS_MAXIMUM((byte)0x22),
    /**
     * 0x23	主题别名	双字节整数	PUBLISH
     */
    TOPIC_ALIAS((byte)0x23),
    /**
     * 0x24	最大QoS	字节	CONNACK
     */
    MAXIMUM_QOS((byte)0x24),
    /**
     * 0x25	保留属性可用性	字节	CONNACK
     */
    RETAIN_AVAILABLE((byte)0x25),

    /**
     * 0x26	用户属性	UTF-8字符串对	CONNECT, CONNACK, PUBLISH, Will Properties, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, DISCONNECT, AUTH
     */
    USER_PROPERTY((byte)0x26),
    /**
     * 0x27	最大报文长度	四字节整数	CONNECT, CONNACK
     */
    MAXIMUM_PACKET_SIZE((byte)0x27),

    /**
     * 0x28	通配符订阅可用性	字节	CONNACK
     */
    WILDCARD_SUBSCRIPTION_AVAILABLE((byte)0x28),

    /**
     * 0x29	订阅标识符可用性	字节	CONNACK
     */
    SUBSCRIPTION_IDENTIFIER_AVAILABLE((byte)0x29),
    /**
     * 0x2A	共享订阅可用性	字节	CONNACK
     */
    SHARED_SUBSCRIPTION_AVAILABLE((byte)0x2A),

    ;

    private final byte code;

    public static PropertiesCode valueOf(byte b) {
        for (PropertiesCode propertiesCode : PropertiesCode.values()) {
            if (propertiesCode.code == b) {
                return propertiesCode;
            }
        }
        return null;
    }
}
