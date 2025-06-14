package com.tlmqtt.auth.acl;

import com.tlmqtt.auth.acl.local.LocalAclValidator;
import com.tlmqtt.common.enums.Action;
import com.tlmqtt.common.model.TlMqttSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * @Author: hszhou
 * @Date: 2025/6/2 14:40
 * @Description: 权限控制器
 */
@Slf4j
public class AclManager {


    private final LocalAclValidator localAclValidator;

    @Setter
    private String path;

     public AclManager() {
         localAclValidator = new LocalAclValidator("acl.conf");
     }



    public AclManager(String path) {
        localAclValidator = new LocalAclValidator(path);
    }
    /**
     * 校验是否有订阅的权限
     * @author hszhou
     * @datetime: 2025-06-02 14:58:27
     * @param session 会话
     * @return boolean
     **/
    public boolean checkSubscribePermission(TlMqttSession session,String topic){

        TlAclRequest request = TlAclRequest.builder().ip(session.getIp()).username(session.getUsername())
            .client(session.getClientId()).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkSubscribe(request);
    }

    public boolean checkPublishPermission(String clientId,String username,String ip,String topic) {
        TlAclRequest request = TlAclRequest.builder().ip(ip).username(username)
            .client(clientId).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkPublish(request);
    }
}
