package com.tlmqtt.common.model.entity;

import lombok.Data;

/**
 * @Author: hszhou
 * @Date: 2025/5/24 13:35
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Data
public class PubrelMessage {

    private String clientId;

    private Long messageId;

}
