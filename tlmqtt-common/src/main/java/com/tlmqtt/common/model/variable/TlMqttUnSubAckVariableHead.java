package com.tlmqtt.common.model.variable;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@Builder
public class TlMqttUnSubAckVariableHead {

    private int messageId;

}
