package com.tlmqtt.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hszhou
 * @Date: 2024/12/2 13:16
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
public class TlLog {

    /**
     * 打印完整字节的
     * @author hszhou
     * @datetime: 2025-05-13 16:25:36
     * @param prefix 消息前缀
     * @param buf bytebuffer
     **/
    public static void logger(String prefix, ByteBuf buf){
        String sb = ByteBufUtil.hexDump(buf);
        log.debug("【{}】 16 【{}】", prefix, sb);
    }

}
