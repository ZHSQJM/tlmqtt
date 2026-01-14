package com.tlmqtt.common.model.response;

import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.variable.TlMqttConnackVariableHead;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@SuperBuilder
@Slf4j
public class TlMqttConnackAck extends AbstractTlMessage {

    private TlMqttConnackVariableHead variableHead;

    public static TlMqttConnackAck build(int sessionPresent, MqttErrorCode returnCode, MqttVersion mqttVersion,String clientId,short keepAlive,  MqttConfiguration mqttConfiguration){
        TlMqttConnackVariableHead variableHead =TlMqttConnackVariableHead.builder().currentSession(sessionPresent).code(returnCode.byteValue()).build();
        int propertiesLength = 0;
        if(mqttVersion == MqttVersion.MQTT_5){
            //如果会话过期间隔（Session Expiry Interval）值未指定，则使用CONNECT报文中指定的会话过期时间间隔。服务端使用此属性通知客户端它使用的会话过期时间间隔与客户端在CONNECT中发送的值不同。
            variableHead.setSessionExpiryInterval(mqttConfiguration.getInt(MqttConfiguration.SESSION_EXPIRY_INTERVAL));
            //服务端使用此值限制服务端愿意为该客户端同时处理的QoS为1和QoS为2的发布消息最大数量。没有机制可以限制客户端试图发送的QoS为0的发布消息。 如果没有设置最大接收值，将使用默认值6553
            variableHead.setReceiveMaximum((short)200);
            //最大服务质量 Maximum QoS 如果没有设置最大服务质量，客户端可使用最大QoS为2。
            variableHead.setMaximumQoS(mqttConfiguration.getInt(MqttConfiguration.MAXIMUM_QOS));
            variableHead.setRetainAvailable(mqttConfiguration.getBoolean(MqttConfiguration.RETAIN_AVAILABLE)?(byte) 1:(byte) 0);
            variableHead.setMaximumPacketSize(mqttConfiguration.getInt(MqttConfiguration.MAXIMUM_PACKET_SIZE));
            variableHead.setAssignedClientIdentifier(clientId);
            variableHead.setTopicAliasMaximum((short)mqttConfiguration.getInt(MqttConfiguration.TOPIC_ALIAS_MAXIMUM).intValue());
            variableHead.setReasonString(null);
            variableHead.setUserProperties(null);
            variableHead.setWildcardSubscriptionsAvailable(mqttConfiguration.getBoolean(MqttConfiguration.WILDCARD_SUBSCRIPTION_AVAILABLE));
            variableHead.setSubscriptionIdentifiersAvailable(mqttConfiguration.getBoolean(MqttConfiguration.SUBSCRIPTION_IDENTIFIERS_AVAILABLE));
            variableHead.setSharedSubscriptionAvailable(mqttConfiguration.getBoolean(MqttConfiguration.SHARED_SUBSCRIPTION_AVAILABLE));
            variableHead.setServerKeepAlive(keepAlive);
            variableHead.setResponseInformation(null);
            variableHead.setServerReference(null);
            variableHead.setAuthenticationMethod(null);
            variableHead.setAuthenticationData(null);
            propertiesLength = calculatePropertiesLength(variableHead);
            variableHead.setPropertiesLength(propertiesLength);
        }
        //最少2个字节 1个字节的sessionPresent和1个字节的返回码
        int remainingLength = 2;
        if (propertiesLength > 0) {
            remainingLength += calculateVariableByteIntegerLength(propertiesLength) + propertiesLength;
        }
        TlMqttFixedHead fixedHead= TlMqttFixedHead.builder().length(remainingLength).messageType(MqttMessageType.CONNACK).build();
        return TlMqttConnackAck.builder().fixedHead(fixedHead).variableHead(variableHead).build();
    }

    @Override
    public MqttMessageType getMessageType() {
        return MqttMessageType.CONNACK;
    }

    /**
     * 计算属性部分长度
     *
     * @param variableHead 可变头
     * @return 属性长度
     */
    private static int calculatePropertiesLength(TlMqttConnackVariableHead variableHead) {
        int propertiesLength = 0;

        // 会话到期间隔 (必选)  // 1字节标识 + 4字节值
        propertiesLength += 5;

        // 接收最大值 (可选)  // 1字节标识 + 2字节值
        if (variableHead.getReceiveMaximum() != null) {
            propertiesLength += 3;
        }

        // QoS最大值 (可选)  // 1字节标识 + 1字节值
        if (variableHead.getMaximumQoS() != null) {
            propertiesLength += 2;
        }

        // 保留可用 (必选) // 1字节标识 + 1字节值
        propertiesLength += 2;

        // 最大报文大小 (可选) // 1字节标识 + 4字节值
        if (variableHead.getMaximumPacketSize() != null) {
            propertiesLength += 5;
        }

        // 分配客户端ID (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getAssignedClientIdentifier() != null) {
            byte[] clientIdBytes = variableHead.getAssignedClientIdentifier().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + clientIdBytes.length;
        }

        // 主题别名最大值 (可选)  // 1字节标识 + 2字节值
        if (variableHead.getTopicAliasMaximum() != null) {
            propertiesLength += 3;
        }

        // 原因字符串 (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getReasonString() != null) {
            byte[] reasonBytes = variableHead.getReasonString().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + reasonBytes.length;
        }

        // 用户属性 (可选) // 1字节标识 + 2字节key长度 + key + 2字节value长度 + value
        List<UserProperty> userProperties = variableHead.getUserProperties();
        if (userProperties != null && !userProperties.isEmpty()) {
            for (UserProperty userProperty : userProperties) {
                byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                propertiesLength += 3 + keyBytes.length + 2 + valueBytes.length;
            }
        }

        // 通配符订阅可用 (必选)  // 1字节标识 + 1字节值
        propertiesLength += 2;

        // 订阅标识符可用 (必选) // 1字节标识 + 1字节值
        propertiesLength += 2;

        // 共享订阅可用 (必选) // 1字节标识 + 1字节值
        propertiesLength += 2;

        // 服务器保活时间 (可选)  // 1字节标识 + 2字节值
        if (variableHead.getServerKeepAlive() != null) {
            propertiesLength += 3;
        }

        // 响应信息 (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getResponseInformation() != null) {
            byte[] responseInfoBytes = variableHead.getResponseInformation().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + responseInfoBytes.length;
        }

        // 服务器参考 (可选)  // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getServerReference() != null) {
            byte[] serverRefBytes = variableHead.getServerReference().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + serverRefBytes.length;
        }

        // 认证方法 (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getAuthenticationMethod() != null) {
            byte[] authMethodBytes = variableHead.getAuthenticationMethod().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + authMethodBytes.length;
        }

        // 认证数据 (可选) // 1字节标识 + 2字节长度 + 字符串
        if (variableHead.getAuthenticationData() != null) {
            byte[] authDataBytes = variableHead.getAuthenticationData().getBytes(StandardCharsets.UTF_8);
            propertiesLength += 3 + authDataBytes.length;
        }

        return propertiesLength;
    }

}
