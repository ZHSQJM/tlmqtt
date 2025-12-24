package com.tlmqtt.common.model;


import com.tlmqtt.common.MessageIdManager;
import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.entity.UserProperty;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class TlMqttSession {
    /**客户端标识*/
    private String clientId;
    /**断开时 是否清除会话*/
    private boolean cleanSession;
    /**订阅的主题*/
    private Set<String> topics;
    /**协议版本*/
    private MqttVersion mqttVersion;
    /**用户名*/
    private String username;
    /**ip*/
    private String ip;
    /**keepAlive*/
    private Short keepAlive;
    /**channel*/
    private ChannelHandlerContext ctx;
    /**是否从存储中恢复会话*/
    private boolean fromStore;


    /**会话的过期时间
     * 1.如果未指定 则为0 如果为0的时候 断开后直接会话结束
     * 2. 如果是0xFFFFFFFF 则永远不会过期
     * 3. 如果不为0  则关闭会话的时候保存会话
     * 1. 如果cleanSession为1 且sessionExpiryInterval为0 则等同于3.1.1中的cleansession为1
     * 2. 如果cleanSession为0 且sessionExpiryInterval为0 则等同于3.1.1中的cleansession为0
     * */
    private int sessionExpiryInterval;

    /**客户端愿意同时处理的QoS 1/2消息最大数量
     * 限制服务器发送速率，防止客户端过载
     * 低性能设备可设置较小值(如10)
     * 高性能设备可设置较大值(如65535)
     * 不能为0  如果为0 就协议错误
     * */
    private Short receiveMaximum;

    /**客户端能接受的最大报文长度(字节)
     * 内存受限设备可设置较小值(如1024字节)
     * 限制服务器发送大报文，避免内存溢出
     * 不能设置0 如果为0 协议错误
     * */
    private Integer maximumPacketSize;

    /**客户端愿意接受的主题别名最大数量
     * 减少长主题名的网络传输
     * 高频发布场景可显著节省带宽
     * */
    private Short topicMaxAlias;

    /**请求详情信息
     * 客户端是否请求服务器返回响应信息
     * */
    private boolean requestResponseInformation;

    /**客户端是否希望收到详细的错误信息
     * */
    private boolean requestProblemInformation;

    /**
     *
     * 自定义键值对字符串(UTF-8)
     传递设备位置信息
     携带设备固件版本
     传输自定义元数据*/
    private List<UserProperty> userProperties;

    /**
     * 是否是5版本
     * @return boolean 是否
     */
    public boolean isVersion5() {
        return mqttVersion == MqttVersion.MQTT_5;
    }

    // --- 核心状态管理组件 ---

    /**
     * 每个会话独享的消息ID生成器
     * 确保分配出的 ID 在 1-65535 之间且不重复
     */
    private final MessageIdManager messageIdManager = new MessageIdManager();

    /**
     * 当前正在传输中（未收到 ACK）的消息计数
     * 对应 MQTT 5.0 的 Receive Maximum
     */
    private final AtomicInteger inFlightCount = new AtomicInteger(0);

    /**
     * 消息积压队列
     * 当 inFlightCount 达到上限时，后续消息进入此队列
     */
    private final Queue<TlMqttPublishReq> messageQueue = new ConcurrentLinkedQueue<>();



}
