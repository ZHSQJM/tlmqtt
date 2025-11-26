package com.tlmqtt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * mqtt的版本枚举
 *
 * @author hszhou
 */
@Getter
@AllArgsConstructor
public enum MqttVersion {

    /**
     * MQTT 3.1.1
     */
    MQTT_3_1((byte)3),
    MQTT_3_1_1 ((byte)4),
    MQTT_5((byte)5);

    private final byte level;


    public static MqttVersion valueOf(byte b) {
        for (MqttVersion mqttVersion : values()) {
            if (mqttVersion.level == b) {
                return mqttVersion;
            }
        }
        return null;
    }

}
