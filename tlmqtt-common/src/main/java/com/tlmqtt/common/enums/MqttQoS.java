package com.tlmqtt.common.enums;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 14:44
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public enum MqttQoS {

    /**
     * 最多一次
     */
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2),
    FAILURE(128);

    private final int value;

    private MqttQoS(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static MqttQoS valueOf(int value) {
        return switch (value) {
            case 0 -> AT_MOST_ONCE;
            case 1 -> AT_LEAST_ONCE;
            case 2 -> EXACTLY_ONCE;
            case 128 -> FAILURE;
            default -> throw new IllegalArgumentException("invalid QoS: " + value);
        };
    }
}
