package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 粘性订阅选择器：对同一 content 始终路由到同一订阅者（基于稳定哈希）
 **/
public class StickySubscribeClientChoose implements IShareSubscribeClientChoose {
    @Override
    public TlSubClient choose(List<TlSubClient> subClients, String content) {
        if (subClients == null || subClients.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(content == null ? 0 : content.hashCode(), subClients.size());
        return subClients.get(index);
    }
}
