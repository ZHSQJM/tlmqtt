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
public class TlMqttPubCompVariableHead {

    private Long messageId;

    /**mqtt5.0 协议有的*/
    private byte reasonCode;

    private String reasonString;

    private List<UserProperty> userPropertyList;

    /**属性长度*/
    private int propertiesLength;


}
