package com.tlmqtt.common.model.variable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class TlMqttUnSubAckVariableHead {

    private int messageId;

    public static TlMqttUnSubAckVariableHead build(int messageId){
        return new TlMqttUnSubAckVariableHead(messageId);
    }

}
