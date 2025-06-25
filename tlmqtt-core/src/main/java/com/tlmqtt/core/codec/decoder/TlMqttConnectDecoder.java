package com.tlmqtt.core.codec.decoder;


import com.tlmqtt.common.enums.MqttMessageType;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.request.AbstractTlMessage;
import com.tlmqtt.common.model.request.TlMqttConnectReq;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
public class TlMqttConnectDecoder extends AbstractTlMqttDecoder{



    @Override
    public TlMqttConnectReq build(ByteBuf buf, int type, int remainingLength) {
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttConnectVariableHead variableHead = decodeVariableHeader(buf);
        TlMqttConnectPayload payload = decodePayLoad(buf, variableHead.getWillFlag(), variableHead.getUsernameFlag(), variableHead.getWillQos());
        return new TlMqttConnectReq(fixedHead, variableHead, payload);
    }


    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        TlMqttFixedHead fixedHead = new TlMqttFixedHead();
        fixedHead.setMessageType(MqttMessageType.CONNECT);
        fixedHead.setLength(remainingLength);
        return fixedHead;
    }


    TlMqttConnectVariableHead decodeVariableHeader(ByteBuf buf) {
        TlMqttConnectVariableHead variableHead = new TlMqttConnectVariableHead();
        int protocolLength = buf.readUnsignedShort();
        variableHead.setProtocolNameLength(protocolLength);
        byte[] protocolName = new byte[4];
        buf.readBytes(protocolName);
        short version = buf.readUnsignedByte();
        variableHead.setProtocolVersion(version);
        //连接标识
        int connectFlag = buf.readUnsignedByte();
        int reserved = (connectFlag) & 1;
        variableHead.setReserved(reserved);
        int clearSession = (connectFlag >> 1) & 1;
        variableHead.setCleanSession(clearSession);
        int willFlag = (connectFlag >> 2) & 1;
        variableHead.setWillFlag(willFlag);
        int willQos = (connectFlag >> 3) & 3;
        variableHead.setWillQos(willQos);
        int willRetain = (connectFlag >> 5) & 1;
        variableHead.setWillRetain(willRetain);
        int passwordFlag = (connectFlag >> 6) & 1;
        variableHead.setPasswordFlag(passwordFlag > 0);
        int usernameFlag = (connectFlag >> 7) & 1;
        variableHead.setUsernameFlag(usernameFlag > 0);
        short keepAlive = buf.readShort();
        variableHead.setKeepAlive(keepAlive);
        log.trace("Parse【CONNECT】message :protocol=【{}】,version=【{}】,reserved=【{}】,cleanSession=【{}】," +
                        "willFlag=【{}】,willQos=【{}】,willRetain=【{}】,usernameFlag=【{}】,keepAlive=【{}】",
                new String(protocolName), version, reserved, clearSession,
                willFlag, willQos, willRetain, usernameFlag, keepAlive);
        return variableHead;
    }

    TlMqttConnectPayload decodePayLoad(ByteBuf buf, int willFlag, boolean usernameFlag, int willQos) {
        int clientIdLength = buf.readUnsignedShort();
        byte[] clientIdByte = new byte[clientIdLength];
        buf.readBytes(clientIdByte);
        String clientId = new String(clientIdByte);
        TlMqttConnectPayload connectPayload = new TlMqttConnectPayload();
        connectPayload.setClientId(clientId);
        String willTopic = "";
        String willMessage = "";
        String username = "";
        String password = "";
        if (willFlag == 1) {
            int willTopicLength = buf.readUnsignedShort();
            byte[] willTopicByte = new byte[willTopicLength];
            buf.readBytes(willTopicByte);
            willTopic = new String(willTopicByte);
            connectPayload.setWillTopic(willTopic);

            int messageLength = buf.readUnsignedShort();
            byte[] messageByte = new byte[messageLength];
            buf.readBytes(messageByte);
            willMessage = new String(messageByte);
            connectPayload.setWillMessage(willMessage);
        }

        if (usernameFlag) {
            int usernameLength = buf.readUnsignedShort();
            byte[] usernameByte = new byte[usernameLength];
            buf.readBytes(usernameByte);
            username = new String(usernameByte);
            connectPayload.setUsername(username);

            int passwordLength = buf.readUnsignedShort();
            byte[] passwordByte = new byte[passwordLength];
            buf.readBytes(passwordByte);
            password = new String(passwordByte);
            connectPayload.setPassword(password);
        }
        log.trace("Parse【CONNECT】payload: clientId=【{}】,willFlag=【{}】,willTopic=【{}】，retain=【{}】，username=【{}】", clientId, willFlag, willQos, willTopic, username);
        return connectPayload;
    }
}
