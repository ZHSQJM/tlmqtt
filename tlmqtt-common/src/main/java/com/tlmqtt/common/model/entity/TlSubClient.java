package com.tlmqtt.common.model.entity;

import lombok.AllArgsConstructor;
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
public class TlSubClient {


    @EqualsAndHashCode.Exclude
    private int qos;

    private String clientId;

    private String topic;
}
