package com.tlmqtt.common.model.request;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */

@Data
@SuperBuilder
public abstract class AbstractTlMessage {


    private TlMqttFixedHead fixedHead;


    /**
     * 获取消息类型
     * @author hszhou
     * @since  2025-05-20 18:04:39
     * @return MqttMessageType
     **/
    public  abstract  MqttMessageType getMessageType();

    /**
     * 计算value占用几个字节
     */
    public static int calculateVariableByteIntegerLength(int value) {
        if (value == 0) {
            return 1;
        }
        int length = 0;
        do {
            length++;
            value >>>= 7;
        } while (value > 0);
        return length;
    }

}
