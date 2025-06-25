package com.tlmqtt.common.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * 加载配置文件
 *
 * @author hszhou
 */
@Data
public class MqttConfiguration {


    private TlMqttProperties mqttProperties;


    public  MqttConfiguration() {
        Yaml yaml = new Yaml(new Constructor(TlMqttProperties.class));
        mqttProperties = yaml.load(this.getClass().getClassLoader().getResourceAsStream("talent.yml"));
    }

    public static void main(String[] args) {
        MqttConfiguration mqttConfiguration = new MqttConfiguration();
        TlMqttProperties mqttProperties1 = mqttConfiguration.getMqttProperties();
        TlAuthProperties authProperties = mqttProperties1.getAuth();
    }
}
