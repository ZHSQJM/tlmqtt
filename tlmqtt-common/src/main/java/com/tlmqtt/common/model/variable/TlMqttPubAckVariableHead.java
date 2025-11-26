package com.tlmqtt.common.model.variable;

import com.tlmqtt.common.model.entity.UserProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@Builder
public class TlMqttPubAckVariableHead {

    private Long messageId;
    /**
     * 响应码 5.0 下面都是5.0
     * 0	0x00	成功	消息被接收。QoS为1的消息已发布。
     * 16	0x10	无匹配的订阅者	消息被接收，但没有订阅者。只有服务端会发送此原因码。如果服务端得知没有匹配的订阅者，服务端可以使用此原因码代替0x00（成功）。
     * 128	0x80	未指明的错误	接收端不接受此消息，且不愿意透露错误原因或没有适用的原因码。
     * 131	0x83	实现特定错误	PUBLISH报文有效，但不被接收端所接受。
     * 135	0x87	未授权	PUBLISH报文未授权。
     * 144	0x90	主题名无效	主题名格式正确，但未被客户端或服务端所接受。
     * 145	0x91	报文标识符被占用	报文标识符已被占用。可能表明客户端和服务端之间的会话状态不匹配。
     * 151	0x97	超出配额	已超出实现限制或管理限制。
     * 153	0x99	载荷格式无效	载荷格式与载荷格式指示符不匹配。
     */

    private byte reasonCode;

    private String reasonString;

    private List<UserProperty> userPropertyList;


    /**属性长度*/
    private int propertiesLength;

}
