package com.tlmqtt.core.task;

import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.store.service.SessionService;
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

    private TlStoreManager storeManager;

    private volatile boolean cancelled = false;

    private volatile Timeout timeout;

    public TlSessionTask(String clientId,TlStoreManager storeManager) {
        this.clientId = clientId;
        this.storeManager = storeManager;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        log.info("开始删除会话");
        storeManager.clearAll(clientId);
    }


    public void cancel(){
        cancelled = true;
        if(timeout !=null && !timeout.isCancelled()){
            timeout.cancel();
        }
    }
}
