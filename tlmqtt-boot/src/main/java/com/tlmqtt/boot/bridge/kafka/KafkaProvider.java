package com.tlmqtt.boot.bridge.kafka;

import com.tlmqtt.bridge.db.TlMySqlInfo;
import com.tlmqtt.bridge.kafka.TlKafkaInfo;

/**
 * @Author: hszhou
 * @Date: 2025/5/29 15:28
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class KafkaProvider {

    public static TlKafkaInfo kafkaInfo() {


        return new TlKafkaInfo("ws", "172.28.33.102:9092",
            "org.apache.kafka.common.serialization.StringSerializer",
            "org.apache.kafka.common.serialization.StringSerializer");
    }
}
