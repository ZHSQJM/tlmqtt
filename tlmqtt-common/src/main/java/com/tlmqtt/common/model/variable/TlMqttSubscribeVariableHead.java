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
public class TlMqttSubscribeVariableHead {

    private int messageId;

    /**
     * 订阅标识符
     */
    private int subscriptionIdentifier;

    /**
     * 用户属性
     */
    private List<UserProperty> userPropertyList;

}
