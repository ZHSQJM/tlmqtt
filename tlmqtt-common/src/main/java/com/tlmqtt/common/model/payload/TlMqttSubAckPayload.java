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

    public static TlMqttSubAckPayload build(int[] codes) {
//        int[] codes = new int[topics.size()+failure.size()];
//        for (int i = 0; i < topics.size(); i++) {
//            codes[i]=topics.get(i).getQos();
//        }
//        for (int i = 0; i < failure.size(); i++) {
//            codes[i+topics.size()]=0x80;
//        }

       return new TlMqttSubAckPayload(codes);
    }

    public static TlMqttSubAckPayload buildFailure() {
        int[] codes = new int[1];
        codes [0] = 0x80;
        return new TlMqttSubAckPayload(codes);
    }

}
