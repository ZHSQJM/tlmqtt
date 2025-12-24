package com.tlmqtt.core.task;


import com.tlmqtt.store.service.session.SessionService;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hszhou
 */
@Slf4j
@Data
public class TlSessionTask implements TimerTask {


    private String clientId;

  //  private TlStoreManager storeManager;
    private SessionService sessionService;

    private volatile boolean cancelled = false;

    private volatile Timeout timeout;

    public TlSessionTask(String clientId,SessionService storeManager) {
        this.clientId = clientId;
        this.sessionService = storeManager;
    }

    @Override
    public void run(Timeout timeout) throws Exception {

        sessionService.clearAll(clientId);
    }


    public void cancel(){
        cancelled = true;
        if(timeout !=null && !timeout.isCancelled()){
            timeout.cancel();
        }
    }
}
