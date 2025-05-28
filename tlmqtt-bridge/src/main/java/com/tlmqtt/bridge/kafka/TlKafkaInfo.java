package com.tlmqtt.bridge.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hszhou
 * @Date: 2025/5/12 14:51
 * @Description: kafka的发送者配置
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlKafkaInfo {

    private String topic;

    private String bootstrapServers;

    private String keySerializer;

    private String valueSerializer;



}
