package com.tlmqtt.sink.kafka;

import com.tlmqtt.common.sink.SinkProvider;
import com.tlmqtt.common.sink.BaseSinkEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
@Slf4j
public class KafkaSinkProviderProvider implements SinkProvider {

    private final ConcurrentHashMap<String, KafkaProducer> connectionMap = new ConcurrentHashMap<>(10);
    @Override
    public void process(BaseSinkEntity sinkEntity, Map<String, Object> params) throws Exception {
        if( !(sinkEntity instanceof TlKafkaInfo)){
            return ;
        }
        TlKafkaInfo kafkaInfo = (TlKafkaInfo) sinkEntity;
        String bootstrapServers = kafkaInfo.getBootstrapServers();
        KafkaProducer kafkaProducer = connectionMap.get(bootstrapServers);
        String topic = kafkaInfo.getTopic();
        if(kafkaProducer == null){
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaInfo.getBootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            kafkaProducer = new KafkaProducer<>(props);

        }
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, params);
        kafkaProducer.send(record);
    }

}




