package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;
import io.netty.channel.Channel;

import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface IShareSubscribeClientChoose {


    /**
     * 选择一个session
     * @param channels 会话
     * @return 选择的session
     */
    TlSubClient choose(List<TlSubClient> channels);
}
