package com.tlmqtt.bridge.kafka;

import com.lmax.disruptor.EventHandler;
import com.tlmqtt.common.model.entity.PublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hszhou
 * @Date: 2025/1/21 15:49
 * @Description: kafka桥接数据
 */
@Slf4j
public class KafkaBridgeObserver  implements EventHandler<PublishMessage> {

    private final ConcurrentHashMap<String, Producer<String, String>> producerPool = new ConcurrentHashMap<>();

    private final List<TlKafkaInfo> list = new ArrayList<>();

    public void add(Object object) {

        if (object instanceof TlKafkaInfo kafkaInfo) {
            try {
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaInfo.getBootstrapServers());
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                // 可选优化配置
                // 消息确认机制
                props.put(ProducerConfig.ACKS_CONFIG, "all");
                // 重试次数
                props.put(ProducerConfig.RETRIES_CONFIG, 3);
                // 幂等性
                props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
                KafkaProducer<String, String> producer = new KafkaProducer<>(props);
                producerPool.put(kafkaInfo.getBootstrapServers(), producer);
                this.list.add(kafkaInfo);
            }
            catch (Exception e) {
                log.error("connect kafka url【{}】 fail", kafkaInfo.getBootstrapServers(), e);
            }
        }

    }

    @Override
    public void onEvent(PublishMessage event, long sequence, boolean endOfBatch) throws Exception {
        for (TlKafkaInfo entityInfo : list) {
            Producer<String, String> producer = producerPool.get(entityInfo.getBootstrapServers());
            // 3. 构造消息记录
            ProducerRecord<String, String> record = new ProducerRecord<>(entityInfo.getTopic(),
                event.toString());
            try {
                RecordMetadata metadata = producer.send(record).get();
                log.info("消息发送成功，partition:{}, offset:{}", metadata.partition(), metadata.offset());
            }catch (Exception e) {
                log.error("消息发送失败", e);
            }
        }
    }
}
