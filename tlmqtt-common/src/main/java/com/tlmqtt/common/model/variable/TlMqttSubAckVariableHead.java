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
public class TlMqttSubAckVariableHead  {

    private int messageId;

    private String reasonString;

    private List<UserProperty> userPropertyList;

}
