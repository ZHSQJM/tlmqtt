package com.tlmqtt.rule.rule;

import com.tlmqtt.common.enums.MqttDataSource;
import com.tlmqtt.common.rule.EventContext;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class OfflineRecordSourceRule extends BaseRule{
    @Override
    public boolean shouldProcess(EventContext context) {
        return context.getDataSource() == MqttDataSource.CLIENT_DISCONNECTED;
    }
}
