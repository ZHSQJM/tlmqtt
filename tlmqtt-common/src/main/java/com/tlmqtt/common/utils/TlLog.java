package com.tlmqtt.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
public class TlLog {

    /**
     * 打印完整字节的
     * @author hszhou
     * @since  time: 2025-05-13 16:25:36
     * @param prefix 消息前缀
     * @param buf bytebuffer
     **/
    public static void logger(String prefix, ByteBuf buf){
       String sb = ByteBufUtil.hexDump(buf);
        log.debug("【{}】 ===【{}】", prefix, sb);
    }

    }


    // Received packet: {"cmd":"publish","retain":false,"qos":1,"dup":false,"length":19,"topic":"testtopic/1","payload":{"type":"Buffer","data":[49]},"messageId":1,"properties":{"payloadFormatIndicator":false}}
//                      {"cmd":"publish","retain":false,"qos":1,"dup":false,"length":19,"topic":"testtopic/1","payload":{"type":"Buffer","data":[49]},"messageId":2,"properties":{"payloadFormatIndicator":true}}