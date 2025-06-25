package com.tlmqtt.common.model.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author hszhou
 */
@Data
@Builder
public class PubrelMessage {

    private String clientId;

    private Long messageId;

}
