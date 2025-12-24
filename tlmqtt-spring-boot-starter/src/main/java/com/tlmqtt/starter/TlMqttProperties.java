package com.tlmqtt.starter;

import com.tlmqtt.common.properties.TlMqttServerProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@ConfigurationProperties(prefix = "tlmqtt")
@Configuration
@Getter
@Setter
@ToString
@EnableAutoConfiguration
@Component
public class TlMqttProperties extends TlMqttServerProperties {


}
