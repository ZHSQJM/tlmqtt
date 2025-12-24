package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 基于消息内容的hash值选择一个订阅会话
 **/
public class HashSubscribeClientChoose implements IShareSubscribeClientChoose{


    @Override
    public TlSubClient choose(List<TlSubClient> subClients,String content) {

        // 检查列表是否为空
        if (subClients == null || subClients.isEmpty()) {
            return null;
        }

       return subClients.get(ThreadLocalRandom.current().nextInt(subClients.size()));
    }
}
