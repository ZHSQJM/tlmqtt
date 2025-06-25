package com.tlmqtt.common.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 主题类
 *
 * @author hszhou
 */
@Data
@EqualsAndHashCode
@ToString
public class TlTopic {

    @EqualsAndHashCode.Include
    private String name;

    private int qos;


}

