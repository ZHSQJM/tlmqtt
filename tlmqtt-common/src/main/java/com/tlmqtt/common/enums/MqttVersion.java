package com.tlmqtt.common.enums;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 14:44
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public enum MqttVersion {

    /**
     * MQTT 3.1.1
     */
    MQTT_3_1("MQIsdp", (byte)3),
    MQTT_3_1_1("MQTT", (byte)4),
    MQTT_5("MQTT", (byte)5);

    private final String name;
    private final byte level;

    private MqttVersion(String protocolName, byte protocolLevel) {
        this.name = (String) ObjectUtil.checkNotNull(protocolName, "protocolName");
        this.level = protocolLevel;
    }

    public String protocolName() {
        return this.name;
    }

    public byte[] protocolNameBytes() {
        return this.name.getBytes(CharsetUtil.UTF_8);
    }

    public byte protocolLevel() {
        return this.level;
    }

    public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel) {
        MqttVersion mv = null;
        switch (protocolLevel) {
            case 3:
                mv = MQTT_3_1;
                break;
            case 4:
                mv = MQTT_3_1_1;
                break;
            case 5:
                mv = MQTT_5;
        }

        if (mv == null) {
            throw new RuntimeException(protocolName + " is an unknown protocol name");
        } else if (mv.name.equals(protocolName)) {
            return mv;
        } else {
            throw new RuntimeException(protocolName + " and " + protocolLevel + " don't match");
        }
    }
}
