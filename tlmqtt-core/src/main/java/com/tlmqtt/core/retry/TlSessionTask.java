package com.tlmqtt.core.retry;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 * @Author: hszhou
 * @Date: 2025/5/8 18:54
 * @Description: 定时关闭session的cleansession为0的客户端
 */
public class TlSessionTask implements TimerTask {


    private String clientId;


    @Override
    public void run(Timeout timeout) throws Exception {

    }
}
