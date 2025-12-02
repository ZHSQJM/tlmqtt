package com.tlmqtt.common.model.response;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.variable.TlMqttConnackVariableHead;
import lombok.*;
import lombok.experimental.Accessors;
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
@Accessors
@SuperBuilder
@Slf4j
public class TlMqttConnackAck extends AbstractTlMessage {

    private TlMqttConnackVariableHead variableHead;

    public static TlMqttConnackAck build(int currentSession, boolean existsSession, MqttErrorCode returnCode, MqttVersion mqttVersion,String clientId,short keepAlive){
        /*如果服务端接受了一个CleanSession设置为1的连接，服务端必须将CONNACK包中的Session Present设置为0，并且CONNACK包的返回码也设置为0。
         如果服务端接受了一个CleanSession设置为0的连接，Session Present的值取决于服务端是否已经存储了客户端Id对应的绘画状态。如果服务端已经存储了会话状态，CONNACK包中的Session Present必须设置为1[MQTT-3.2.2-2]。如果服务端没有存储会话状态，CONNACK包的Session Present必须设置为0。另外CONNACK包中的返回码必须设为0[MQTT-3.2.2-3]。
         Session Present标识使得客户端能够建立连接，不论客户端和服务端在是否已经存储了会话状态上达成共识。*/
        int sessionPresent = 0;
        if(currentSession==0){
            sessionPresent = existsSession?1:0;
        }

        TlMqttConnackVariableHead variableHead =TlMqttConnackVariableHead.builder().currentSession(sessionPresent).code(returnCode.byteValue()).build();
        int propertiesLength = 0;
        if(mqttVersion == MqttVersion.MQTT_5){
            variableHead.setSessionExpiryInterval(Constant.SESSION_EXPIRY_INTERVAL);
            variableHead.setReceiveMaximum((short)200);
            variableHead.setMaximumQoS(null);
            variableHead.setRetainAvailable((byte) 1);
            variableHead.setMaximumPacketSize(65535);
            variableHead.setAssignedClientIdentifier(clientId);
            variableHead.setTopicAliasMaximum((short) Constant.TOPIC_ALIAS_MAXIMUM);
            variableHead.setReasonString(null);
            variableHead.setUserProperties(null);
            variableHead.setWildcardSubscriptionsAvailable(true);
            variableHead.setSubscriptionIdentifiersAvailable(true);
            variableHead.setSharedSubscriptionAvailable(true);
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
