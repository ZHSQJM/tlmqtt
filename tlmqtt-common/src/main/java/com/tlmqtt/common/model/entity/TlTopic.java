package com.tlmqtt.common.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 18:33
 * @Description: 主题类
 */
@Data
@EqualsAndHashCode
@ToString
public class TlTopic {

    @EqualsAndHashCode.Include
    private String name;

    private int qos;


}

