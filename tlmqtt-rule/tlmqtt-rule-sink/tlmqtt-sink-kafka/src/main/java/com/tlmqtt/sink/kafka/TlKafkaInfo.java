package com.tlmqtt.sink.kafka;

import com.tlmqtt.common.sink.BaseSinkEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
 */

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlKafkaInfo extends BaseSinkEntity {

    private String topic;

    private String bootstrapServers;

    private String keySerializer;

    private String valueSerializer;


}
