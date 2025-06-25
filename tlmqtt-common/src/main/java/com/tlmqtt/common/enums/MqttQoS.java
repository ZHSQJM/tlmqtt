package com.tlmqtt.common.enums;

/**
 * mqtt的qos等级
 *
 * @author hszhou
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
        MqttQoS mqttQos;
        switch (value) {
            case 0:
                mqttQos = AT_MOST_ONCE;
                break;
            case 1:
                mqttQos = AT_LEAST_ONCE;
                break;
            case 2:
                mqttQos = EXACTLY_ONCE;
                break;
            case 128:
                mqttQos = FAILURE;
                break;
            default:
                throw new IllegalArgumentException("invalid QoS: " + value);
        }
        return mqttQos;
    }
}
