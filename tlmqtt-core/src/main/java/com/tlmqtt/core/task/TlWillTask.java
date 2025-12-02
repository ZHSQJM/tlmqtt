package com.tlmqtt.core.task;

import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.manager.MessageManager;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author hszhou
 */
@Slf4j
@Data
public class TlWillTask  implements TimerTask {

    private final String clientId;

    private final MessageManager messageManager;

    private final TlMqttPublishReq req;

    private final int delay;

    private volatile boolean cancelled = false;

    private volatile Timeout timeout;

    public TlWillTask(String clientId,MessageManager messageManager,TlMqttPublishReq req,int delay) {
        this.clientId = clientId;
        this.messageManager = messageManager;
        this.req = req;
        this.delay = delay;
    }


    @Override
    public void run(Timeout timeout) throws Exception {
        messageManager.publish(req,clientId, MqttVersion.MQTT_5);
    }


    public void cancel(){
        cancelled = true;
        if(timeout !=null && !timeout.isCancelled()){
            timeout.cancel();
        }
    }
}
