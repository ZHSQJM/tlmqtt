package com.tlmqtt.common;

import com.tlmqtt.common.model.TlMqttSession;
import io.netty.util.AttributeKey;

import java.util.Arrays;
import java.util.List;

/**
 * @author hszhou
 */
public class Constant {

    /**
     * 协议名称
     */
    public static final String PROTOCOL_NAME = "MQTT";



    /**
     * 保存到通道中的断开连接是否发送了disconnect报文
     */
    public static final String DISCONNECT = "disconnect_flag";
    public static final String MQTT_SESSION = "session";
    public static final AttributeKey<Boolean> DISCONNECT_KEY = AttributeKey.valueOf(Constant.DISCONNECT);
    public static final AttributeKey<TlMqttSession> SESSION_KEY = AttributeKey.valueOf(Constant.MQTT_SESSION);


    /**
     * 消息位移的位数
     */
    public static final int MESSAGE_BIT = 4;
    public static final String TOPIC_SPLITTER = "#";
    public static final String TOPIC_WILDCARD = "+";
    public static final String TOPIC_SPLITTER_SPLITTER = "\\/";
    public  static final String COLON = "\\:";
    public static final String ASTERISK = "*";
    public static final String COMMA = "\\,";
    public static final String VERTICAL_LINE ="\\|";


    /**分组订阅的前缀*/
    public static final String SHARE_PREFIX_SUBSCRIBE = "$share/";

    public static final String QUEUE_PREFIX_SUBSCRIBE = "$queue/";

    /**错误的QOS等级*/
    public static final int ERROR_QOS = 3;

    /**
     * acl控制的类型
     */
    public static final String CLIENT = "client";
    public static final String IP = "ip";
    public static final String USER = "user";

    /**优雅停机后的时间*/
    public static final long TIMEOUT = 10;

    public static final String WILL = "will";
    public static final String PUBLISH = "publish";
    public static final String PUBREL = "pubrel";

    ;
}
