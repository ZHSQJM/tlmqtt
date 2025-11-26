package com.tlmqtt.common.model.variable;

import com.tlmqtt.common.model.entity.UserProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * mqtt的请求可变头
 * 协议名称 协议 连接标识 保持连接
 *
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@Builder
public class TlMqttConnackVariableHead{

    /**连接确认标识*/
    private int currentSession;

    /**连接返回码*/
    private int code;

    /**会话过期时间*/
    private int sessionExpiryInterval;

    /**接收最大值*/
    private Short receiveMaximum;

    /**最大服务质量*/
    private Integer maximumQoS;

    /**保留可用*/
    private byte retainAvailable;

    /**最大报文长度*/
    private Integer maximumPacketSize;

    /**分配客户端标识符*/
    private String assignedClientIdentifier;

    /**主题别名最大值*/
    private Short topicAliasMaximum;

    /**原因字符串*/
    private String reasonString;

    /**用户属性*/
    private List<UserProperty> userProperties;

    /**通配符订阅可用*/
    private boolean wildcardSubscriptionsAvailable;

    /**订阅标识符订阅可用*/
    private boolean subscriptionIdentifiersAvailable;

    /**共享订阅可用*/
    private boolean sharedSubscriptionAvailable;

    /**服务器KeepAlive*/
    private Short serverKeepAlive;

    /**响应信息*/
    private String responseInformation;

    /**服务端参考*/
    private String serverReference;

    /**认证方法*/
    private String authenticationMethod;

    /**认证数据*/
    private String authenticationData;


    /**属性长度*/
    private int propertiesLength;


}
