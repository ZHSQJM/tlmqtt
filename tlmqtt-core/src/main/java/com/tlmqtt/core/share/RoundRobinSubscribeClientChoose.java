package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 轮询选择一个订阅会话
 **/
public class RoundRobinSubscribeClientChoose implements IShareSubscribeClientChoose {
    
    /**使用原子整数来保证线程安全的轮询计数*/
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public TlSubClient choose(List<TlSubClient> subClients,String content) {
        // 检查列表是否为空
        if (subClients == null || subClients.isEmpty()) {
            return null;
        }
        
        // 获取当前计数并增加，确保原子性
        int index = Math.abs(counter.getAndIncrement() % subClients.size());
        
        // 返回轮询选中的会话
        return subClients.get(index);
    }
}