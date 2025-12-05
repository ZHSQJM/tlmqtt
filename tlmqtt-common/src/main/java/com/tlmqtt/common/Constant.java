package com.tlmqtt.common;

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
     * 保存在通道中客户端的KEY
     */
    public static final String CLIENT_ID = "client_id";

    /**
     * 保存到通道中的断开连接是否发送了disconnect报文
     */
    public static final String DISCONNECT = "disconnect_flag";

    /**
     * 保存在通道中的消息KEY 用于在接收pubrel的时候获取对应的消息
     */
    public static final String PUB_MSG = "pub_msg";

    /**
     * 消息位移的位数
     */
    public static final int MESSAGE_BIT = 4;


    public static final String USERNAME = "username";

    public static final String IP ="ip";

    public static final String TOPIC_SPLITTER = "#";
    public static final String TOPIC_WILDCARD = "\\+";
    public static final String TOPIC_SPLITTER_SPLITTER = "\\/";
    public  static final String COLON = "\\:";
    public static final String ASTERISK = "*";
    public static final String COMMA = "\\,";
    public static final String VERTICAL_LINE ="\\|";

    public static final String MQTT_SESSION = "session";

    /**分组订阅的前缀*/
    public static final String SHARE_PREFIX_SUBSCRIBE = "$share";




    /**会话最长的过期时间*/
    public static final int SESSION_EXPIRY_INTERVAL = 3600;
    /**服务器支持的最大别名的长度*/
    public static final int TOPIC_ALIAS_MAXIMUM = 200;

    /**拒绝的连接客户端*/
    public static final List<String> REFUSE_CLIENTS = Arrays.asList("tom","jack");

    public static final int MAXIMUM_PACKET_SIZE = 65535 ;

    /**最大支持的qos等级*/
    public static final int MAXIMUM_QOS = 1;

    /**保留可用*/
    public static final boolean RETAIN_AVAILABLE = false;

    /**是否支持通配符订阅*/
    public static final boolean WILDCARD_SUBSCRIPTION_AVAILABLE = true;

    /**订阅标识符可用*/
    public static final boolean SUBSCRIPTION_IDENTIFIERS_AVAILABLE = true;

   /**是否支持共享订阅*/
    public static final boolean SHARED_SUBSCRIPTION_AVAILABLE = true;
}
