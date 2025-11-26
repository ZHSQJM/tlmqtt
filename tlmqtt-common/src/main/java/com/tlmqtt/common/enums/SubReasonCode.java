package com.tlmqtt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅的原因码
 * @author hszhou
 */
@Getter
@AllArgsConstructor
public enum SubReasonCode {


    /**
     * 0	0x00	授予QoS等级0	订阅被接受且最大QoS等级为0。可能低于所请求的QoS等级。
     * 1	0x01	授予QoS等级1	订阅被接受且最大QoS等级为1。可能低于所请求的QoS等级。
     * 2	0x02	授予QoS等级2	订阅被接受且最大QoS等级为2。可能低于所请求的QoS等级。
     * 128	0x80	未指明错误	订阅未被接受，且服务端不愿意透露原因或没有适用的原因码。
     * 131	0x83	实现特定错误	SUBSCRIBE有效但不被服务端所接受。
     * 135	0x87	未授权	客户端未被授权做此订阅。
     * 143	0x8F	主题过滤器无效	主题过滤器格式正确，但不被允许。
     * 145	0x91	报文标识符已被占用	指定的报文标识符正在被使用中。
     * 151	0x97	超出配额	已超出实现限制或管理限制。
     * 158	0x9E	共享订阅不支持	服务端不支持此客户端进行共享订阅。
     * 161	0xA1	订阅标识符不支持	服务端不支持订阅标识符；订阅标识符不被接受。
     * 162	0xA2	通配符订阅不支持	服务端不支持通配符订阅；订阅未被接受。
     * mqtt 3 0x00 - 成功 - 最大QoS为0 0x01 - 成功 - 最大QoS为1 0x02 - 成功 - 最大QoS为2 0x80 - 失败
     */


    QOS0((byte) 0),
    QOS1((byte) 1),
    QOS2((byte) 2),
    FAILURE((byte) 128),
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 131),
    NOT_AUTHORIZED((byte) 135),
    TOPIC_FILTER_INVALID((byte) 143),
    TOPIC_NAME_INVALID((byte) 145),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 151),
    WRITE_COUNT_EXCEEDED((byte) 158),
    MESSAGE_TOO_LARGE((byte) 161),
    PROTOCOL_ERROR((byte) 162),

    ;

    private final byte code;
}
