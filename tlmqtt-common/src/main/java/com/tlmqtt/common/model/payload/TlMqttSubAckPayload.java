package com.tlmqtt.common.model.payload;
import com.tlmqtt.common.model.entity.TlTopic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 14:24
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttSubAckPayload {

    private int[] codes;

    public static TlMqttSubAckPayload build(List<TlTopic> topics) {
        int[] codes = new int[topics.size()];
        for (int i = 0; i < topics.size(); i++) {
            codes[i]=topics.get(i).getQos();
        }

       return new TlMqttSubAckPayload(codes);
    }

}
