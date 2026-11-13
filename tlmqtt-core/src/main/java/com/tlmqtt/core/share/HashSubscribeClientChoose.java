package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 基于 content 的哈希值选择一个订阅会话
 **/
public class HashSubscribeClientChoose implements IShareSubscribeClientChoose {


    @Override
    public TlSubClient choose(List<TlSubClient> subClients, String content) {
        if (subClients == null || subClients.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(content == null ? 0 : content.hashCode(), subClients.size());
        return subClients.get(index);
    }
}
