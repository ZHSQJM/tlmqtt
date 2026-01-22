package com.tlmqtt.source.kafka;

import com.tlmqtt.common.sink.DataSink;
import com.tlmqtt.common.sink.SinkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * @author hszhou
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaDataSink implements DataSink {

    private String topic;

    private KafkaProducer kafkaProducer;
//
//    private String bootstrapServers;
//
//    private String keySerializer;
//
//    private String valueSerializer;

    @Override
    public String getId() {
        return "";
    }

    @Override
    public void send(String data) {

        if(kafkaProducer != null){
            kafkaProducer.send(new ProducerRecord<>(topic, data));
        }
    }

    @Override
    public void close() {

        if(kafkaProducer != null){
            kafkaProducer.close();
        }
    }

    @Override
    public SinkType getType() {
        return SinkType.KAFKA;
    }
}
