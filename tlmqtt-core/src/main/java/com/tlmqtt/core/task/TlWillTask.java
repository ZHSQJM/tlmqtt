package com.tlmqtt.core.task;

import com.tlmqtt.common.enums.MqttVersion;
import com.tlmqtt.common.model.request.TlMqttPublishReq;
import com.tlmqtt.core.service.ForwardMessageService;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hszhou
 */
@Slf4j
@Data
public class TlWillTask  implements TimerTask {

    private final String clientId;

    private final ForwardMessageService forwardMessageService;

    private final TlMqttPublishReq req;

    private final int delay;

    private volatile boolean cancelled = false;

    private volatile Timeout timeout;

    public TlWillTask(String clientId,ForwardMessageService forwardMessageService,TlMqttPublishReq req,int delay) {
        this.clientId = clientId;
        this.forwardMessageService = forwardMessageService;
        this.req = req;
        this.delay = delay;
    }


    @Override
    public void run(Timeout timeout) throws Exception {
        forwardMessageService.publish(req,clientId, MqttVersion.MQTT_5);
    }


    public void cancel(){
        cancelled = true;
        if(timeout !=null && !timeout.isCancelled()){
            timeout.cancel();
        }
    }
}
