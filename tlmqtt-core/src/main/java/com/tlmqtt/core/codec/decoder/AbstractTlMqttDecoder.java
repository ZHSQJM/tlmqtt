package com.tlmqtt.core.codec.decoder;

import com.tlmqtt.common.model.request.AbstractTlMessage;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hszhou
 * @Date: 2025/5/19 9:11
 * @Description: 抽象解码器
 */
public abstract class  AbstractTlMqttDecoder {

    /**
     * 解析完成的数据
     * @author hszhou
     * @datetime: 2025-05-19 09:12:29
     * @param buf 数据
     * @param type 类型
     * @param remainingLength 剩余长度
     * @return AbstractTlMessage
     **/
    public abstract AbstractTlMessage build(ByteBuf buf,int type, int remainingLength);
}
