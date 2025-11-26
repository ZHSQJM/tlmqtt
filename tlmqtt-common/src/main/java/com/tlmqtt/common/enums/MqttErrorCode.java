package com.tlmqtt.common.enums;

/**
 * mqtt的连接拒绝原因
 *
 * @author hszhou
 */
public enum MqttErrorCode {


    // 5.0 服务端不支持客户端所请求的MQTT协议版本
    CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte)0x84),
    // 无效报文
    MALFORMED_MESSAGE((byte) 0x81),
    // 不支持的qos等级
    CONNECTION_REFUSED_QOS_NOT_SUPPORTED((byte)0x9B),
    //如果服务端拒绝了某个客户标识符（ClientID）
    REFUSED_CLIENT_IDENTIFIER((byte)0x85),

    //报文太大
    CONNECTION_REFUSED_MESSAGE_TOO_LARGE((byte)0x95),

    //载荷格式无效
    CONNECTION_REFUSED_PAYLOAD_FORMAT_INVALID((byte)0x99),


    //会话被接管 发送disconnect 如果已经被接管了
    CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED((byte)0x8E),

    //报文过长
    PACKET_TOO_LARGE((byte) 0x95),

    /**
     * 连接成功
     */
    CONNECTION_ACCEPTED((byte)0x00),

    // 3.1.1 协议等级不被服务端支持
    //CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte)0x01),

    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte)2),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte)3),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte)4),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte)5),

    CONNECTION_REFUSED_UNSPECIFIED_ERROR((byte)-128),
    CONNECTION_REFUSED_MALFORMED_PACKET((byte)-127),
    CONNECTION_REFUSED_PROTOCOL_ERROR((byte)-126),
    CONNECTION_REFUSED_IMPLEMENTATION_SPECIFIC((byte)-125),
    CONNECTION_REFUSED_UNSUPPORTED_PROTOCOL_VERSION((byte)-124),
    CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID((byte)-123),
    CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD((byte)-122),
    CONNECTION_REFUSED_NOT_AUTHORIZED_5((byte)-121),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE_5((byte)-120),
    CONNECTION_REFUSED_SERVER_BUSY((byte)-119),
    CONNECTION_REFUSED_BANNED((byte)-118),
    CONNECTION_REFUSED_BAD_AUTHENTICATION_METHOD((byte)-116),
    CONNECTION_REFUSED_TOPIC_NAME_INVALID((byte)-112),
    CONNECTION_REFUSED_PACKET_TOO_LARGE((byte)-107),
    CONNECTION_REFUSED_QUOTA_EXCEEDED((byte)-105),

    CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED((byte)-102),

    CONNECTION_REFUSED_USE_ANOTHER_SERVER((byte)-100),
    CONNECTION_REFUSED_SERVER_MOVED((byte)-99),
    ;



    private static final MqttErrorCode[] VALUES;
    private final byte byteValue;

    private MqttErrorCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    public byte byteValue() {
        return this.byteValue;
    }

    public static MqttErrorCode valueOf(byte b) {
        int unsignedByte = b & 255;
        MqttErrorCode mqttErrorCode = null;

        try {
            mqttErrorCode = VALUES[unsignedByte];
        } catch (ArrayIndexOutOfBoundsException var4) {
        }

        if (mqttErrorCode == null) {
            throw new IllegalArgumentException("unknown connect return code: " + unsignedByte);
        } else {
            return mqttErrorCode;
        }
    }

    static {
        MqttErrorCode[] values = values();
        VALUES = new MqttErrorCode[160];
        for (MqttErrorCode code : values) {
            int unsignedByte = code.byteValue & 255;
            VALUES[unsignedByte] = code;
        }
    }
}
