package com.tlmqtt.boot.config;

import com.tlmqtt.core.TlBootstrap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: hszhou
 * @Date: 2025/5/27 11:19
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Configuration
public class WebConfig {

    @Bean
    public TlBootstrap tlBootstrap(){
        return new TlBootstrap();
    }

}
