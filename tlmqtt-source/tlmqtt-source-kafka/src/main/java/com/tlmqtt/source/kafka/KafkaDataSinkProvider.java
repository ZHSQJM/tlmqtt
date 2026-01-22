//package com.tlmqtt.source.kafka;
//
//import com.tlmqtt.common.sink.DataSink;
//import com.tlmqtt.common.sink.SinkType;
//import com.tlmqtt.common.sink.DataSinkProvider;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.producer.KafkaProducer;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.common.serialization.StringSerializer;
//
//import java.util.Map;
//import java.util.Properties;
//
///**
// * @author zhouhs
// * @version 0.1.0
// * @since 0.1.0
// **/
//@Data
//@Slf4j
//public class KafkaDataSinkProvider implements DataSinkProvider {
//
//    @Override
//    public SinkType getType() {
//        return SinkType.KAFKA;
//    }
//
//    @Override
//    public DataSink createSink(Map<String, Object> config) {
//        String bootstrapServers = (String) config.get("bootstrap.servers");
//        String topic = (String) config.get("topic");
//        String id = (String) config.get("id"); // 区分不同 Kafka 实例的 ID
//
//        return new KafkaDataSink(id, bootstrapServers, topic);
//    }
//
//    @Override
//    public boolean init(DataSink tlSourceBean) {
//        if( !(tlSourceBean instanceof KafkaDataSink)){
//            return false;
//        }
//        KafkaDataSink kafkaInfo = (KafkaDataSink) tlSourceBean;
//        Properties props = new Properties();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaInfo.getBootstrapServers());
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        props.put(ProducerConfig.ACKS_CONFIG, "all");
//        props.put(ProducerConfig.RETRIES_CONFIG, 3);
//        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//        this.producer = new KafkaProducer<>(props);
//        this.topic = kafkaInfo.getTopic();
//        return true;
//    }
//
//    @Override
//    public void forwardData(Object object) {
//        if (producer != null) {
//            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, object);
//            producer.send(record);
//        }
//    }
//
//    @Override
//    public void close() {
//
//        if(producer != null){
//            producer.close();
//        }
//    }
//
//    @Override
//    public SinkType name() {
//        return SinkType.KAFKA;
//    }
//}
