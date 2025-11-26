package com.tlmqtt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * pub的原因码
 * @author hszhou
 */
@Getter
@AllArgsConstructor
public enum PubReasonCode {

    /**0	0x00	成功	消息被接收。QoS为1的消息已发布。
     16	0x10	无匹配的订阅者	消息被接收，但没有订阅者。只有服务端会发送此原因码。如果服务端得知没有匹配的订阅者，服务端可以使用此原因码代替0x00（成功）。
     128	0x80	未指明的错误	接收端不接受此消息，且不愿意透露错误原因或没有适用的原因码。
     131	0x83	实现特定错误	PUBLISH报文有效，但不被接收端所接受。
     135	0x87	未授权	PUBLISH报文未授权。
     144	0x90	主题名无效	主题名格式正确，但未被客户端或服务端所接受。
     145	0x91	报文标识符被占用	报文标识符已被占用。可能表明客户端和服务端之间的会话状态不匹配。
     151	0x97	超出配额	已超出实现限制或管理限制。
     153	0x99	载荷格式无效	载荷格式与载荷格式指示符不匹配。*/


    SUCCESS((byte) 0),
    NO_MATCHING_SUBSCRIBERS((byte) 16),
    UNSPECIFIED_ERROR((byte) 128),
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 131),
    NOT_AUTHORIZED((byte) 135),
    TOPIC_NAME_INVALID((byte) 144),
    PACKET_IDENTIFIER_IN_USE((byte) 145),
    QUOTA_EXCEEDED((byte) 151),
    PAYLOAD_FORMAT_INVALID((byte) 153),
    ;

    private final byte code;
}
