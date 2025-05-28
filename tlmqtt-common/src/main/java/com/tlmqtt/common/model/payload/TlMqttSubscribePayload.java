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
public class TlMqttSubscribePayload {

    private List<TlTopic> topics;

}
