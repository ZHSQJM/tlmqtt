package com.tlmqtt.common.model.payload;

import com.tlmqtt.common.model.entity.UserProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnectPayload {

    /** 载体 */
    private String clientId;

    /** 遗嘱topic */
    private String willTopic;

    /** 遗嘱消息 */
    private String willMessage;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /**遗嘱延迟间隔*/
    private Integer willDelayInterval;

    /** 载荷格式指示器 */
    private Boolean payloadFormatIndicator;


    /**消息过期间隔*/
    private Integer messageExpiryInterval;

    /**内容类型*/
    private String contentType;

    /**响应主题*/
    private String responseTopic;

    /** 对比数据 */
    private String correlationData;

    /** 用户属性 */
    private List<UserProperty> userProperty;

}
