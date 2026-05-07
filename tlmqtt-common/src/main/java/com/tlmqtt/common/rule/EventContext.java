package com.tlmqtt.common.rule;

import com.tlmqtt.common.enums.MqttDataSource;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventContext {

    private MqttDataSource dataSource;

    private String clientId;

    private String ip;

    private Long timestamp;

    private TlMqttPublishReq req;
}
