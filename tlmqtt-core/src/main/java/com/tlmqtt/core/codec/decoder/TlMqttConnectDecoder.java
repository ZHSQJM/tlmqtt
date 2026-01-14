package com.tlmqtt.core.codec.decoder;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.config.MqttConfiguration;
import com.tlmqtt.common.enums.MqttErrorCode;
import com.tlmqtt.common.enums.MqttMessageType;

import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.enums.PropertiesCode;
import com.tlmqtt.common.exception.TlMalformedPacketException;
import com.tlmqtt.common.exception.TlMqttException;
import com.tlmqtt.common.exception.TlProtocolErrorException;
import com.tlmqtt.common.exception.UnAcceptableProtocolVersionException;
import com.tlmqtt.common.model.TlMqttSession;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.fix.TlMqttFixedHead;
import com.tlmqtt.common.model.payload.TlMqttConnectPayload;
import com.tlmqtt.common.model.request.TlMqttConnectReq;
import com.tlmqtt.common.model.variable.TlMqttConnectVariableHead;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * @author hszhou
 */
@Slf4j
public class TlMqttConnectDecoder extends AbstractTlMqttDecoder{

    private  int maximumQos;
    private  boolean retainAvailable;
    private  List<String> invalidTopicNames;
    private  List<String> refuseClients;
    private  int maximumPacketSize;

    public  TlMqttConnectDecoder(MqttConfiguration configuration){
        super(configuration);
        this.maximumQos = configuration.getInt(MqttConfiguration.MAXIMUM_QOS);
        this.retainAvailable = configuration.getBoolean(MqttConfiguration.RETAIN_AVAILABLE);
        this.invalidTopicNames = configuration.getList(MqttConfiguration.INVALID_TOPIC_NAMES);
        this.refuseClients = configuration.getList(MqttConfiguration.REFUSE_CLIENTS);
        this.maximumPacketSize = configuration.getInt(MqttConfiguration.MAXIMUM_PACKET_SIZE);

        configuration.addListener(property -> {
            if (property == MqttConfiguration.Property.MAXIMUM_QOS) {
                int newValue = configuration.getInt(MqttConfiguration.Property.MAXIMUM_QOS.getKey());
                log.debug("【TLMQTT】Codec maximumQos hot-updated to: {}", newValue);
                this.maximumQos = newValue;
            }
            if (property == MqttConfiguration.Property.RETAIN_AVAILABLE) {
                boolean newValue = configuration.getBoolean(MqttConfiguration.Property.RETAIN_AVAILABLE.getKey());
                log.debug("【TLMQTT】Codec retainAvailable hot-updated to: {}", newValue);
                this.retainAvailable = newValue;
            }
            if (property == MqttConfiguration.Property.INVALID_TOPIC_NAMES) {
                List<String> newValue = configuration.getList(MqttConfiguration.Property.INVALID_TOPIC_NAMES.getKey());
                log.debug("【TLMQTT】Codec invalidTopicNames hot-updated to: {}", newValue);
                this.invalidTopicNames = newValue;
            }
            if (property == MqttConfiguration.Property.REFUSE_CLIENTS) {
                List<String>  newValue = configuration.getList(MqttConfiguration.Property.REFUSE_CLIENTS.getKey());
                log.debug("【TLMQTT】Codec refuseClients hot-updated to: {}", newValue);
                this.refuseClients = newValue;
            }
            if (property == MqttConfiguration.Property.MAXIMUM_PACKET_SIZE) {
                int newValue = configuration.getInt(MqttConfiguration.Property.MAXIMUM_PACKET_SIZE.getKey());
                log.debug("【TLMQTT】Codec maxPacketSize hot-updated to: {}", newValue);
                this.maximumPacketSize = newValue;
            }
        });
    }
    @Override
    public TlMqttConnectReq build(ByteBuf buf, int type, int remainingLength, TlMqttSession session ) {
        if(buf.readableBytes() > maximumPacketSize){
            throw new TlProtocolErrorException(MqttMessageType.CONNECT, MqttMessageType.CONNACK);
        }
        TlMqttFixedHead fixedHead = decodeFixedHeader(remainingLength);
        TlMqttConnectVariableHead variableHead = decodeVariableHeader(buf);
        TlMqttConnectPayload payload = decodePayload(buf, variableHead.getWillFlag(), variableHead.isUsernameFlag(),variableHead.getProtocolVersion());
         return TlMqttConnectReq.builder()
                                .fixedHead(fixedHead)
                                .variableHead(variableHead)
                                .payload(payload).build();
    }


    TlMqttFixedHead decodeFixedHeader(int remainingLength) {
        return TlMqttFixedHead.builder()
                              .messageType(MqttMessageType.CONNECT)
                              .length(remainingLength).build();
    }


    TlMqttConnectVariableHead decodeVariableHeader(ByteBuf buf ) {
        TlMqttConnectVariableHead.TlMqttConnectVariableHeadBuilder builder = TlMqttConnectVariableHead.builder();
        int protocolLength = buf.readUnsignedShort();
        byte[] protocolNameByte = new byte[protocolLength];
        //协议名称为MQTT
        buf.readBytes(protocolNameByte);
        String protocolName = new String(protocolNameByte);
        builder.protocolName(protocolName);
         //支持多种协议的服务端使用协议名字段判断数据是否为MQTT报文。协议名必须是UTF-8字符串“MQTT”。如果服务端不愿意接受CONNECT但希望表明其MQTT服务端身份，
        // 可以发送包含原因码为0x84（不支持的协议版本）的CONNACK报文，然后必须关闭网络连接 [MQTT-3.1.2-1]。
        if(!Constant.PROTOCOL_NAME.equals(protocolName)){
            throw new UnAcceptableProtocolVersionException();
        }
        short version = buf.readUnsignedByte();
        builder.protocolVersion(version);
        if(version != MqttVersion.MQTT_5.getLevel()  && version != MqttVersion.MQTT_3_1_1.getLevel() ){
            //支持多版本MQTT协议的服务端使用协议版本字段判定客户端正使用的MQTT协议版本。如果协议版本不是5且服务端不愿意接受此CONNECT报文，可以发送包含原因码0x84（不支持的协议版本）的CONNACK报文，然后必须关闭网络连接 [MQTT-3.1.2-2]。
            throw new UnAcceptableProtocolVersionException();
        }
        //连接标识
        int connectFlag = buf.readUnsignedByte();
        int reserved = (connectFlag) & 1;
        if(reserved != 0){
            //服务端必须验证CONNECT控制报文的保留标志位（第0位）是否为0 [MQTT-3.1.2-3]，如果不为0则此报文为无效报文。4.13节给出了错误处理信息。
            //4.13 在CONNECT报文出错的情况下它可以在关闭网络连接之前发送包含原因码的CONNACK报文。在其他报文出错的情况下它应该在关闭网络连接之前发送包含原因码的DISCONNECT报文。使用原因码0x81（无效报文）或0x82（协议错误）
            throw new TlMalformedPacketException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
        }
        builder.reserved(reserved);
        int clearSession = (connectFlag >> 1) & 1;
        builder.cleanSession(clearSession);
        int willFlag = (connectFlag >> 2) & 1;
        builder.willFlag(willFlag);
        int willQos = (connectFlag >> 3) & 3;
        if(willQos == Constant.ERROR_QOS){
            //  不支持的QoS等级
            throw new TlMalformedPacketException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
        }

        //如果服务端收到包含遗嘱的QoS超过服务端处理能力的CONNECT报文，服务端必须拒绝此连接。服务端应该使用包含原因码为0x9B（不支持的QoS等级）的CONNACK报文进行错误处理，随后必须关闭网络连接。
        if(maximumQos <willQos && version == MqttVersion.MQTT_5.getLevel()){
            throw new TlMqttException(MqttErrorCode.CONNECTION_REFUSED_QOS_NOT_SUPPORTED,true,MqttMessageType.CONNECT,null, MqttMessageType.CONNACK);
        }
        builder.willQos(willQos);
        int willRetain = (connectFlag >> 5) & 1;
        //如果服务端收到一个包含保留标志位1的遗嘱消息的CONNECT报文且服务端不支持保留消息，服务端必须拒绝此连接请求，且应该发送包含原因码为0x9A（不支持保留）的CONNACK报文，随后必须关闭网络连接 [MQTT-3.2.2-13]
        if(!retainAvailable&& willRetain==1){
            throw new TlMqttException(MqttErrorCode.CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED,true,MqttMessageType.CONNECT,null,MqttMessageType.CONNACK);
        }
        builder.willRetain(willRetain);
        int passwordFlag = (connectFlag >> 6) & 1;
        builder.passwordFlag(passwordFlag > 0);
        int usernameFlag = (connectFlag >> 7) & 1;
        builder.usernameFlag(usernameFlag > 0);
        short keepAlive = buf.readShort();
        builder.keepAlive(keepAlive);
        log.trace("【TLMQTT】Parse【CONNECT】message :protocol=【{}】,version=【{}】,reserved=【{}】,cleanSession=【{}】,willFlag=【{}】,willQos=【{}】,willRetain=【{}】,usernameFlag=【{}】,keepAlive=【{}】",
            protocolName, version, reserved, clearSession, willFlag, willQos, willRetain, usernameFlag, keepAlive);
        TlMqttConnectVariableHead variableHead = builder.build();
        if(version== MqttVersion.MQTT_5.getLevel()){
            processVariableProperty(buf,variableHead);
        }
        return variableHead ;
    }

    /**
     * 解析变量属性
     * @param buf 字节数组
     * @param variableHead 可变头
     */
    private void processVariableProperty(ByteBuf buf,TlMqttConnectVariableHead variableHead ) {
        //properties的长度
        int propertyLength = decodeRemainingLength(buf);
        // 4. 记录属性读取的起始位置
        final int propertiesStartIndex = buf.readerIndex();
        List<UserProperty> userProperties = new ArrayList<>();
        // 5. 循环读取属性直到达到属性长度
        while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
            byte propertyIdentifier = buf.readByte();
            PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
            switch (Objects.requireNonNull(propertiesCode)) {
                case SESSION_EXPIRY_INTERVAL:
                    // 会话过期间隔
                    // 1. 将cleanStart设置为1 且会话过过期间隔设置为0 相当于3.1.1 将cleanSession设置为1
                    // 2. 将cleanStart设置为0 确会话过期间隔不设置 就相当于3.1.1 将cleanSession设置为0
                    //默认为0但是如果不为0 说明之前设置过了
                    if(variableHead.getSessionExpiryInterval()!=0){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    int sessionExpiryInterval = buf.readInt();
                    variableHead.setSessionExpiryInterval(sessionExpiryInterval);
                    break;
                case RECEIVE_MAXIMUM:
                    // 接收最大值
                    // 客户端通过使用这个值来限制同时并行处理qos1和qos2发布的消息数量，接收最大值只会应用在当前网络链接中，
                    // 跟随其后的是由双字节整数表示的最大接收值。包含多个接收最大值或接收最大值为0将造成协议错误（Protocol Error）。
                    if(variableHead.getReceiveMaximum()!=null){
                        //说明设置过了
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    short receiveMaximum = buf.readShort();
                    if(receiveMaximum==0){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    variableHead.setReceiveMaximum(receiveMaximum);
                    break;
                case MAXIMUM_PACKET_SIZE:

                    // 表示客户端愿意接受的最大报文长度
                    // 如果不存在最大报文长度属性，作为剩余长度编码和协议头大小的记过，除了协议的限制外，不限制数据包大小 如果单个报文中该属性出现了多次，或者当值设置为0时，则协议错误
                    // 服务器不能发送最大报文长度的包给客户端。如果客户端收到了超出限制的报文，那么会视为协议错误，服务器虎仔断开连接的时候返回一个带有0x95（报文太大）原因码的DISCONNECT报文
                    // 如果Packet太大以至于不能正常发送，那么服务器就需要丢弃
                    if(variableHead.getMaximumPacketSize()!=null){
                        //说明设置过了
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    int maximumPacketSize = buf.readInt();
                    if(maximumPacketSize==0){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    variableHead.setMaximumPacketSize(maximumPacketSize);
                    break;
                case TOPIC_ALIAS_MAXIMUM:
                    // 主题别名最大值
                    // 如果单个报文中 该属性出现了多次， 则协议错误， 若未设置主题别名的最大数量 则就将其默认你为0
                    // 该值用来表示客户端从服务器哪里接收到主题别名的最大值，客户端使用该值来限制它愿意在此Connection上保留的主题别名的数量
                    // 服务器不能在给客户端发送的PUBLISH报文中发送超出主题别名最大数量的主题别名
                    // 当值为0时则别是客户端在该链接中不会接受任何主题别名 入股主题别名最大数量不存在或者值为0 则服务器不能发送任何主题别名给客户端
                    if(variableHead.getTopicMaxAlias()!=0){
                        //说明设置过了
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    short topicAliasMaximum= buf.readShort();
                    variableHead.setTopicMaxAlias(topicAliasMaximum);
                    break;
                case REQUEST_RESPONSE_INFORMATION:
                    // 请求响应信息
                    // 如果他表示的值是0或1以外的值 或者该属性出现多次 那么就会视为协议错误，若未指定请求响应消息 则将其值设置默认值0
                    // 客户端使用该值去请求服务器，服务器会在CONNACK包中返回响应信息，
                    // 当请求响应信息设为0的时候，意味着服务器不应该返回详情信息了
                    // 如果值为1 那么服务器会在CONNACK包中返回详情信息
                    byte requestResponseInformation = buf.readByte();
                    if(requestResponseInformation >1){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    variableHead.setRequestResponseInformation(requestResponseInformation==1);
                    break;
                case REQUEST_PROBLEM_INFORMATION:
                    // 请求问题信息
                    // 这个字节只能表示0或者1 如果它表示的值是0或者1以外的值，或者该属性出现了多次，那么就会思维协议错误，若未指定请求详情消息，则将其值设为默认值1
                    // 如果客请求问题信息的值被设为0 服务器在CONNACK或者DISCONNECT报文中返回一个原因字符串或用户属性，但是不能发送原因属性或用户属性在其他的任何包中PUBLISH,CONNACK或DISCONNECT包除外
                    // 如果值设为0 而客户端却在PUBLISH，CONNACK，DISCONNECT包以外收到了原因码或用户属性，那么就应该用一个带有原因码0x82（协议错误）的DISCONNECT报文断开连接
                    // 如果值设置1 那么服务器就可以在任何备允许的报文中返回原因码或用户属性
                    byte requestProblemInformation = buf.readByte();
                    if(requestProblemInformation >1){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    variableHead.setRequestProblemInformation(requestProblemInformation==1);
                    break;
                case USER_PROPERTY:
                    //用户属性
                    int keyLength = buf.readShort();
                    byte[] key = new byte[keyLength];
                    buf.readBytes(key);
                    int valueLength = buf.readShort();
                    byte[] value = new byte[valueLength];
                    buf.readBytes(value);
                    UserProperty userProperty = UserProperty.builder().key(new String(key)).value(new String(value)).build();
                    userProperties.add(userProperty);

                    break;
                case AUTHENTICATION_METHOD:
                    //验证方法
                    break;
                case AUTHENTICATION_DATA:
                    // 认证数据
                    int authDataLength = buf.readUnsignedShort();
                    byte[] authData = new byte[authDataLength];
                    buf.readBytes(authData);
                    break;
                default:
                    // 未知属性，根据规范应跳过
                    break;
            }

            variableHead.setUserProperty(userProperties);
        }



    }

    TlMqttConnectPayload decodePayload(ByteBuf buf, int willFlag, boolean usernameFlag,short version) {
        int clientIdLength = buf.readUnsignedShort();
        /*
         *如果是5.0 协议
         *1. 服务端可以允许客户端提供一个零字节的客户端标识符（clientId） 如果这样做了 服务端必须将这个作为特殊情况 并分配唯一的客户端标识符给那个客户端，
         * 然后它必须假设客户端提供了那个唯一的客户端标识符，正常处理这个CONNECT报文，并且必须返回CONNACK包中被分配的客户端标识符
         *如果服务器拒绝了客户端标识符 那么他可以返回0x85（客户端标识符无效）的原因码的CONNACK报文去相亲CONNECT报文，然后必须关闭网络连接
         * */
        boolean hasClientId = version == MqttVersion.MQTT_3_1_1.getLevel() || (version == MqttVersion.MQTT_5.getLevel() && clientIdLength != 0);
        TlMqttConnectPayload connectPayload = new TlMqttConnectPayload();
        if(hasClientId){
            byte[] clientIdByte = new byte[clientIdLength];
            buf.readBytes(clientIdByte);
            String  clientId = new String(clientIdByte);
            if(refuseClients.contains(clientId)){
                //如果clientId不被服务端接收，那么就返回0x85（客户端标识符无效）的原因码的CONNACK报文去相亲CONNECT报文，然后必须关闭网络连接
                throw new TlMqttException(MqttErrorCode.REFUSED_CLIENT_IDENTIFIER,true,MqttMessageType.CONNECT,null,MqttMessageType.CONNACK);
            }
            connectPayload.setClientId(clientId);
        }

        if (willFlag == 1) {
            if(version== MqttVersion.MQTT_5.getLevel()){
                processPayloadProperty(buf,connectPayload);
            }
            int willTopicLength = buf.readUnsignedShort();
            byte[] willTopicByte = new byte[willTopicLength];
            buf.readBytes(willTopicByte);
            String willTopic = new String(willTopicByte);
            //主题名无效  格式正确 但是不被服务端接收
            if(invalidTopicNames.contains(willTopic)){
                throw new TlMqttException(MqttErrorCode.TOPIC_NAME_INVALID,true,MqttMessageType.CONNECT,null,MqttMessageType.CONNACK);
            }
            connectPayload.setWillTopic(willTopic);

            int messageLength = buf.readUnsignedShort();
            byte[] messageByte = new byte[messageLength];
            buf.readBytes(messageByte);
            String willMessage = new String(messageByte);
            connectPayload.setWillMessage(willMessage);
        }

        if (usernameFlag) {
            int usernameLength = buf.readUnsignedShort();
            byte[] usernameByte = new byte[usernameLength];
            buf.readBytes(usernameByte);
            String username = new String(usernameByte);
            connectPayload.setUsername(username);

            int passwordLength = buf.readUnsignedShort();
            byte[] passwordByte = new byte[passwordLength];
            buf.readBytes(passwordByte);
            String password = new String(passwordByte);
            connectPayload.setPassword(password);
        }
        return connectPayload;
    }

    /**
     * 解析属性
     * @param buf 字节数组
     * @param connectPayload 载荷
     */
    private void processPayloadProperty(ByteBuf buf,TlMqttConnectPayload connectPayload ) {
        int propertyLength = decodeRemainingLength(buf);
        final int propertiesStartIndex = buf.readerIndex();
        // 5. 循环读取属性直到达到属性长度
        while (buf.readerIndex() - propertiesStartIndex < propertyLength) {
            byte propertyIdentifier = buf.readByte();
            PropertiesCode propertiesCode = PropertiesCode.valueOf(propertyIdentifier);
            switch (Objects.requireNonNull(propertiesCode)) {
                case WILL_DELAY_INTERVAL:
                    // 遗嘱延迟间隔
                    // 如果遗嘱延迟间隔没有设置 那么会将默认值设置0 也就意味着遗嘱消息的发布会有有任何延迟
                    // 服务器只有在遗嘱延迟间隔过期或者会话结束的时候才可以发布客户端的遗嘱消息，如果在遗嘱延迟间隔过期之前的在这个会话上建立了一个新的网络连接 那么服务器就不应该再发送任何遗嘱消息了
                    if(connectPayload.getWillDelayInterval()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }

                    int willDelayInterval = buf.readInt();
                    connectPayload.setWillDelayInterval(willDelayInterval);
                    break;
                case PAYLOAD_FORMAT_INDICATOR:
                    // 载荷格式指示
                    // 当该属性值为0的时候 一位置遗嘱消息是未确定的字节，相当于不发送有效载荷格式指示器
                    // 当该属性值为1的时候，意味着遗嘱消息是utf-8的字符数据
                    //包含多个载荷格式指示（Payload Format Indicator）将造成协议错误（Protocol Error）
                    if(connectPayload.getPayloadFormatIndicator()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    // todo 包含多个载荷格式指示（Payload Format Indicator）将造成协议错误（Protocol Error）。服务端可以按照格式指示对遗嘱消息（Will Message）进行验证，如果验证失败发送一条包含原因码0x99（载荷格式无效）的CONNACK报文。如4.13节所述
                    int payloadFormatIndicator = buf.readByte();
                    connectPayload.setPayloadFormatIndicator(payloadFormatIndicator==1);
                    break;
                case MESSAGE_EXPIRY_INTERVAL:
                    //包含多个消息过期间隔将导致协议错误
                    if(connectPayload.getMessageExpiryInterval()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    // 消息过期间隔
                    // 如果存在该属性，那么这4个字节的值就用来表示单位为秒的遗嘱消息的生命周期，并且当服务器饭吧遗嘱消息的时候会被作为发布过去间隔发送
                    int messageExpiryInterval = buf.readInt();
                    connectPayload.setMessageExpiryInterval(messageExpiryInterval);
                    break;
                case CONTENT_TYPE:

                    // 内容类型
                    // 用来描述遗嘱消息内容的字符串
                    //包含多个内容类型（Content Type）将造成协议错误（Protocol Error）
                    if(connectPayload.getContentType()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    int contentLength = buf.readShort();
                    if(contentLength == 0){
                        break;
                    }

                    byte[] willTopicByte = new byte[contentLength];
                    buf.readBytes(willTopicByte);
                    String content = new String(willTopicByte);
                    connectPayload.setContentType(content);
                    break;
                case RESPONSE_TOPIC:
                    // 响应主题
                    // 通常是用来作为响应先
                       //。包含多个响应主题（Response Topic）将造成协议错误
                    if(connectPayload.getResponseTopic()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }

                    int responseTopicLength = buf.readShort();
                    if(responseTopicLength==0){
                        break;
                    }

                    byte[]responseTopicByte = new byte[responseTopicLength];
                    buf.readBytes(responseTopicByte);
                    String responseTopic = new String(responseTopicByte);

                    connectPayload.setResponseTopic(responseTopic);
                    break;
                case CORRELATION_DATA:
                    //包含多个对比数据将造成协议错误（Protocol Error）
                    if(connectPayload.getCorrelationData()!=null){
                        throw new TlProtocolErrorException(MqttMessageType.CONNECT,MqttMessageType.CONNACK);
                    }
                    int correlationDataLength = buf.readShort();
                    byte[]correlationDataByte = new byte[correlationDataLength];
                    buf.readBytes(correlationDataByte);
                    String correlationData = new String(correlationDataByte);
                    connectPayload.setCorrelationData(correlationData);
                    break;
                case USER_PROPERTY:
                    int keyLength = buf.readShort();
                    byte[] key = new byte[keyLength];
                    buf.readBytes(key);
                    int valueLength = buf.readShort();
                    byte[] value = new byte[valueLength];
                    buf.readBytes(value);
                default:
                    // 未知属性，根据规范应跳过
                    break;
            }
        }
    }
}
