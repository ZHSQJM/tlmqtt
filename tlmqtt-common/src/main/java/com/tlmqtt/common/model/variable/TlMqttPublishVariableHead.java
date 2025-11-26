package com.tlmqtt.common.model.variable;

import com.tlmqtt.common.model.entity.UserProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
//@ToString
@Accessors(chain = true)
@Builder
public class TlMqttPublishVariableHead {


    private String topic;

    private Long messageId;

    /**
     * 载荷格式指示器  这个目前没有任何猪柳蛋broker对齐做了限制
     * 1 表示载荷格式是UTF-8的字符数据
     * 0 表示载荷格式是二进制数据
     * 0x01
     */
    private Boolean payloadFormatIndicator;

    /**
     * 消息过期时间
     * 消息过期时间其表示该消息在broker上存在的时间，超过这个时间后broker会删除该消息
     */
    private Integer  messageExpiryInterval;

    /**
     * 主题别名
     */
    private Integer topicAlias;

    /**
     * 响应主题
     */
    private String responseTopic;

    /**
     * 对比数据
     */
    private String correlationData;

    /**
     * 用户属性
     */
    private List<UserProperty> userProperties;

    /**
     * 订阅标识符
     */
    private Integer subscriptionIdentifier;

    private String contentType;

    private int propertiesLength;

    /**遗嘱消息的延迟*/
    private Integer willDelayInterval;
}
