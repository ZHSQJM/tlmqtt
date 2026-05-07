package com.tlmqtt.common.sink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseSinkEntity {

    private SinkType sinkType;

    private Integer id;
}
