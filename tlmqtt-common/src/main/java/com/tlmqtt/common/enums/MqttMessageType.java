package com.tlmqtt.common.enums;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 14:50
 * @Description: mqtt的消息类型
 */
public enum MqttMessageType {

    /***/
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    AUTH(15);

    private static final MqttMessageType[] VALUES;
    private final int value;

    private MqttMessageType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static MqttMessageType valueOf(int type) {
        if (type > 0 && type < VALUES.length) {
            return VALUES[type];
        } else {
            throw new IllegalArgumentException("unknown message type: " + type);
        }
    }

    static {
        MqttMessageType[] values = values();
        VALUES = new MqttMessageType[values.length + 1];
        MqttMessageType[] var1 = values;
        int var2 = values.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            MqttMessageType mqttMessageType = var1[var3];
            int value = mqttMessageType.value;
            if (VALUES[value] != null) {
                throw new AssertionError("value already in use: " + value);
            }

            VALUES[value] = mqttMessageType;
        }

    }
}
