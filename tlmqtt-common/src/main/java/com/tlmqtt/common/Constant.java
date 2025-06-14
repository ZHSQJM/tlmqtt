package com.tlmqtt.common;

/**
 * @Author: hszhou
 * @Date: 2025/5/14 15:44
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class Constant {

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

    //public static final String SESSION = "session" ;
    
    public static final String TOPIC_SPLITTER = "#";
    public static final String TOPIC_WILDCARD = "\\+";
    public static final String TOPIC_SPLITTER_SPLITTER = "\\/";
    public  static final String COLON = "\\:";
    public static final String ASTERISK = "*";
    public static final String COMMA = "\\,";
    public static final String VERTICAL_LINE ="\\|";
}
