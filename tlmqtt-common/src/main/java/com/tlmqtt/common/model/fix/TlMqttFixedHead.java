package com.tlmqtt.common.model.fix;

import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttQoS;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * mqtt的固定头部信息
 *
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
//@ToString
@Builder
@Accessors(chain = true)
public class TlMqttFixedHead {


    /**消息类型*/
    private MqttMessageType messageType;
    /**重发标志位 0 是新消息  1肯能是重发*/
    private boolean dup;
    /**qos的等级*/
    private MqttQoS qos;
    /**是否保留消息*/
    private boolean retain;
    /**消息长度*/
    private int length;

    public static TlMqttFixedHead build(MqttMessageType messageType,MqttQoS qos,boolean retain){
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setDup(false);
        fixedHead.setMessageType(messageType);
        fixedHead.setQos(qos);
        fixedHead.setRetain(retain);
        return fixedHead;
    }


    public static TlMqttFixedHead build(MqttMessageType messageType,MqttQoS qos,boolean dup,boolean retain){
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setDup(dup);
        fixedHead.setMessageType(messageType);
        fixedHead.setQos(qos);
        fixedHead.setRetain(retain);
        return fixedHead;
    }

    /**
     * 构建固定头部信息
     * @param messageType 消息类型
     * @return 固定头部信息
     */
    public static TlMqttFixedHead build(MqttMessageType messageType){
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setDup(false);
        fixedHead.setMessageType(messageType);
        fixedHead.setQos(MqttQoS.AT_MOST_ONCE);
        fixedHead.setRetain(false);
        return fixedHead;
    }



}
