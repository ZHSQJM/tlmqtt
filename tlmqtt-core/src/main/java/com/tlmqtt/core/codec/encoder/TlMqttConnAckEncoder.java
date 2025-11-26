package com.tlmqtt.core.codec.encoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.response.TlMqttConnackAck;
import com.tlmqtt.common.model.variable.TlMqttConnackVariableHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author hszhou
 */
@ChannelHandler.Sharable
@Slf4j
public class TlMqttConnAckEncoder extends AbstractTlMqttEncoder<TlMqttConnackAck> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TlMqttConnackAck res, ByteBuf out, MqttMessageType messageType)  {

        TlMqttFixedHead fixedHead = res.getFixedHead();
        TlMqttConnackVariableHead variableHead = res.getVariableHead();
        // 3. 写入固定头部
        out.writeByte(fixedHead.getMessageType().value() << 4);
        writeVariableByteInteger(out, fixedHead.getLength());
        // 4. 写入CONNACK固定部分
        out.writeByte(variableHead.getCurrentSession());
        out.writeByte(variableHead.getCode());
        // 5. 写入MQTT 5.0属性部分
        int propertiesLength = variableHead.getPropertiesLength();
        if ( propertiesLength > 0) {
            writeVariableByteInteger(out,propertiesLength );
            writeProperties(variableHead, out);
        }
    }

    /**
     * 写入属性
     * @param variableHead 可变头
     * @param out ByteBuf数据
     */
    private void writeProperties(TlMqttConnackVariableHead variableHead, ByteBuf out) {
        // 会话到期间隔
        out.writeByte(PropertiesCode.SESSION_EXPIRY_INTERVAL.getCode());
        out.writeInt(variableHead.getSessionExpiryInterval());

        // 接收最大值
        if (variableHead.getReceiveMaximum() != null) {
            out.writeByte(PropertiesCode.RECEIVE_MAXIMUM.getCode());
            out.writeShort(variableHead.getReceiveMaximum());
        }

        // QoS最大值
        if (variableHead.getMaximumQoS() != null) {
            out.writeByte(PropertiesCode.MAXIMUM_QOS.getCode());
            out.writeByte(variableHead.getMaximumQoS());
        }

        // 保留可用
        out.writeByte(PropertiesCode.RETAIN_AVAILABLE.getCode());
        out.writeByte(variableHead.getRetainAvailable());

        // 最大报文大小
        if (variableHead.getMaximumPacketSize() != null) {
            out.writeByte(PropertiesCode.MAXIMUM_PACKET_SIZE.getCode());
            out.writeInt(variableHead.getMaximumPacketSize());
        }

        // 分配客户端ID
        if (variableHead.getAssignedClientIdentifier() != null) {
            out.writeByte(PropertiesCode.ASSIGNED_CLIENT_IDENTIFIER.getCode());
            byte[] clientIdBytes = variableHead.getAssignedClientIdentifier().getBytes(StandardCharsets.UTF_8);
            out.writeShort(clientIdBytes.length);
            out.writeBytes(clientIdBytes);
        }

        // 主题别名最大值
        if (variableHead.getTopicAliasMaximum() != null) {
            out.writeByte(PropertiesCode.TOPIC_ALIAS_MAXIMUM.getCode());
            out.writeShort(variableHead.getTopicAliasMaximum());
        }

        // 原因字符串
        if (variableHead.getReasonString() != null) {
            out.writeByte(PropertiesCode.REASON_STRING.getCode());
            byte[] reasonBytes = variableHead.getReasonString().getBytes(StandardCharsets.UTF_8);
            out.writeShort(reasonBytes.length);
            out.writeBytes(reasonBytes);
        }

        // 用户属性
        List<UserProperty> userProperties = variableHead.getUserProperties();
        if (userProperties != null && !userProperties.isEmpty()) {
            for (UserProperty userProperty : userProperties) {
                out.writeByte(PropertiesCode.USER_PROPERTY.getCode());
                byte[] keyBytes = userProperty.getKey().getBytes(StandardCharsets.UTF_8);
                out.writeShort(keyBytes.length);
                out.writeBytes(keyBytes);
                byte[] valueBytes = userProperty.getValue().getBytes(StandardCharsets.UTF_8);
                out.writeShort(valueBytes.length);
                out.writeBytes(valueBytes);
            }
        }

        // 通配符订阅可用
        out.writeByte(PropertiesCode.WILDCARD_SUBSCRIPTION_AVAILABLE.getCode());
        out.writeByte(variableHead.isWildcardSubscriptionsAvailable() ? 0x01 : 0x00);

        // 订阅标识符可用
        out.writeByte(PropertiesCode.SUBSCRIPTION_IDENTIFIER_AVAILABLE.getCode());
        out.writeByte(variableHead.isSubscriptionIdentifiersAvailable() ? 0x01 : 0x00);

        // 共享订阅可用
        out.writeByte(PropertiesCode.SHARED_SUBSCRIPTION_AVAILABLE.getCode());
        out.writeByte(variableHead.isSharedSubscriptionAvailable() ? 0x01 : 0x00);

        // 服务器保活时间
        if (variableHead.getServerKeepAlive() != null) {
            out.writeByte(PropertiesCode.SERVER_KEEP_ALIVE.getCode());
            out.writeShort(variableHead.getServerKeepAlive());
        }

        // 响应信息
        if (variableHead.getResponseInformation() != null) {
            out.writeByte(PropertiesCode.REQUEST_INFORMATION.getCode());
            byte[] responseInfoBytes = variableHead.getResponseInformation().getBytes(StandardCharsets.UTF_8);
            out.writeShort(responseInfoBytes.length);
            out.writeBytes(responseInfoBytes);
        }

        // 服务器参考
        if (variableHead.getServerReference() != null) {
            out.writeByte(PropertiesCode.SERVER_REFERENCE.getCode());
            byte[] serverRefBytes = variableHead.getServerReference().getBytes(StandardCharsets.UTF_8);
            out.writeShort(serverRefBytes.length);
            out.writeBytes(serverRefBytes);
        }

        // 认证方法
        if (variableHead.getAuthenticationMethod() != null) {
            out.writeByte(PropertiesCode.AUTHENTICATION_METHOD.getCode());
            byte[] authMethodBytes = variableHead.getAuthenticationMethod().getBytes(StandardCharsets.UTF_8);
            out.writeShort(authMethodBytes.length);
            out.writeBytes(authMethodBytes);
        }

        // 认证数据
        if (variableHead.getAuthenticationData() != null) {
            out.writeByte(PropertiesCode.AUTHENTICATION_DATA.getCode());
            byte[] authDataBytes = variableHead.getAuthenticationData().getBytes(StandardCharsets.UTF_8);
            out.writeShort(authDataBytes.length);
            out.writeBytes(authDataBytes);
        }
    }

}
