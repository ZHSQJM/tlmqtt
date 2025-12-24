package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 粘性订阅选择器
 **/
public class StickySubscribeClientChoose implements IShareSubscribeClientChoose {
    @Override
    public TlSubClient choose(List<TlSubClient> subClients, String content) {
        return null;
    }
}
