package com.tlmqtt.source.kafka;

import com.tlmqtt.common.source.AbstractTlSourceBean;
import com.tlmqtt.common.source.TlSourceProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
@Slf4j
public class KafkaSourceProvider implements TlSourceProvider {

    KafkaProducer<String, Object> producer;

    private String topic;


    @Override
    public boolean init(AbstractTlSourceBean tlSourceBean) {
        if( !(tlSourceBean instanceof TlKafkaInfo)){
            return false;
        }
        TlKafkaInfo kafkaInfo = (TlKafkaInfo) tlSourceBean;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaInfo.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        this.producer = new KafkaProducer<>(props);
        return true;
    }

    @Override
    public void forwardData(Object object) {
        if (producer != null) {
            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, object);
            producer.send(record);
        }
    }

    @Override
    public void close() {

        if(producer != null){
            producer.close();
        }
    }
}
