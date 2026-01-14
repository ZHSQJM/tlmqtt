package com.tlmqtt.source.kafka;

import com.tlmqtt.common.source.AbstractTlSourceBean;
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
public class TlKafkaInfo extends AbstractTlSourceBean {

    private String topic;

    private String bootstrapServers;

    private String keySerializer;

    private String valueSerializer;



}
