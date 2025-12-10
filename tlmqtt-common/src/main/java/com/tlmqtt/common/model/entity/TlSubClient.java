package com.tlmqtt.common.model.entity;

import com.tlmqtt.common.enums.MqttVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 订阅的主题对应的客户端
 *
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TlSubClient {


    @EqualsAndHashCode.Exclude
    private Integer qos;

    private String clientId;

    private String topic;

    private MqttVersion mqttVersion;

    /**订阅标识符*/
    private Integer subscriptionIdentifier;

    /**是否是共享订阅*/
    private Boolean isShared;

    /**是否支持本地转发 也就是自己发送给自己*/
    private Boolean noLocal;

    /**保留处理方式*/
    private Boolean retainAsPublished;

    private String group;


}
