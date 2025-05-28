package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author: hszhou
 * @Date: 2024/11/25 11:13
 * @Description:
 * mqtt的请求可变头
 * 协议名称 协议 连接标识 保持连接
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttConnectVariableHead {

    /**协议名称长度*/
    private Integer protocolNameLength;
    /**协议名称*/
    private String protocolName;
    /**协议版本*/
    private Short protocolVersion;
    //下面是一个字节的连接标识
    /**
     * 如果usernameFlag 设置为0  那么用户名不必须要出现在载荷中
     * 如果usernameFlag设置为1 那么用户名必须出现在在载荷中
     * */
    private Boolean usernameFlag;
    /**
     * 吐过passwordFlag 设置为0  那么密码不必要出现在载荷中
     * 吐过passwordFlag 设置为1 那么密码必须出现在载荷中
     * 如果usernameFlag 设置为0 那么passwordFlag必须设置为0
     * */
    private Boolean passwordFlag;
    /**
     * 表示will message咋发布之后是否需要保留
     * 如果will flag设置我0 那么这个will retain 特必须是0
     * 如果will flag设置为1
     *   如果will retain 设置为0 那么服务端必须发布will Message 不必保存
     *   如果will retain 设置为 那么服务端必须发布will message 并保存
     * */
    private int willRetain;
    /**
     * 连接标识中断bit 4和3
     * 表示will message时 使用的qos登陆
     * 如果willFlag 是0 那么will qos也必须设置为0
     * 如果will flag设置为 1那么will qos的值可以是0  1 2
     * */
    private int willQos;

    /**是否包含遗嘱主题消息的标志位
     * 如果设置1 如果链接请求被接受 服务端必须存储一个will message 并和网络连接关联起来
     * 如果设置为0  连接标识众的will qos和will retain字段必须设置为0
     *  并且will topic 和will message 字段不嫩狗出现在载荷众
     * */
    private int willFlag;
    /**
     * 是否清理之前的会话 开启新会话的标志位
     * 如果cleanSession被设置了0 服务器必须根据当前的会话状态恢复与客户端的通信。
     *       客户端的唯一标识作为会话的标识
     *       如果没有与客户端唯一标识相关的会话，服务端必须创建一个新的会话
     *       客户端和服务端在断开连接后必须存储会话
     *       服务器还必须将所有和客户端订阅相关的qos1和qos2的消息作为一部分存储起来，也可以选择吧qos0的消息存储起亚
     * 如果cleanSession被设置了1 客户端和服务端必须断开之前的回话启动一个新的会话，只要网络连接存在会话就存在
     * */
    private int cleanSession;
    /**预留字段
     * 服务端必须验证CONNECT控制包的预留字段是否为0
     * 如果不为0 断开与客户端的连接
     * */
    private int reserved;

    /**
     * 以秒为单位  指客户端从发送完成一个控制包到开始发送下一个的最大时间间隔
     * 客户端有这人确保两个控制包发送的间隔不能超过keepAlive的值，如果没有其他控制包可发
     *  客户端必须发送PINGREQ吧哦
     * 客户端可以再任何时间发送了PINGREQ后，在一个合理的时间都没有收到PINGRES包，客户端应该关闭和服务端的网络连接
     * Keep Alive的值为0 就关闭了维持的机制，这意味着在这种情况下，服务端不会断开静默的客户端
     */
    private Short keepAlive;


}
