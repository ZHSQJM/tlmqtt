package com.tlmqtt.core.share;

import com.tlmqtt.common.model.entity.TlSubClient;


import java.util.List;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public interface IShareSubscribeClientChoose {


    /**
     * 选择一个session
     * @param subClients 订阅的client列表
     * @param content 消息内容
     * @return 选择的session
     */
    TlSubClient choose(List<TlSubClient> subClients,String content);
}
