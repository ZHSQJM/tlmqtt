package com.tlmqtt.common.model.payload;

import com.tlmqtt.common.model.entity.TlTopic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttUnSubscribePayload {

    private List<TlTopic> topics;


}
