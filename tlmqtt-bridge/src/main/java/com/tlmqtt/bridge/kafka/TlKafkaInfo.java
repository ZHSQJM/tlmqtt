package com.tlmqtt.bridge.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
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
