package com.tlmqtt.common.utils;

import com.tlmqtt.common.enums.MqttMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName：TlLogUtils
 * @Author: hszhou
 * @Date: 2024/12/2 13:16
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
public class TlLog {


    private final static Logger logger = LoggerFactory.getLogger(TlLog.class);


    /**
     * 打印完整字节的
     * @author hszhou
     * @datetime: 2025-05-13 16:25:36
     * @param prefix 消息前缀
     * @param buf bytebuf
     **/
    public static void logger(String prefix, ByteBuf buf){
        String sb = ByteBufUtil.hexDump(buf);
        logger.debug("【{}】 16 【{}】", prefix, sb);

    }

    public static void main(String[] args) {

        int num= MqttMessageType.PUBLISH.value() << 4;
        String binaryStr = Integer.toBinaryString(num);


        // 将二进制数的第二位和第三位变为01
        int result01 = num & ~(3 << 4) | (1 << 5);
        System.out.println("第二位和第三位变为01后的二进制数: " + Integer.toBinaryString(result01));

        // 将二进制数的第二位和第三位变为10
        int result10 = num & ~(3 << 4) | (2 << 4);
        System.out.println("第二位和第三位变为10后的二进制数: " + Integer.toBinaryString(result10));
    }
}
