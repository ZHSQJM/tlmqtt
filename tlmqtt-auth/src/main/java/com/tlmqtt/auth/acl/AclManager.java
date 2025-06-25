package com.tlmqtt.auth.acl;

import com.tlmqtt.auth.acl.local.LocalAclValidator;
import com.tlmqtt.common.enums.Action;
import com.tlmqtt.common.model.TlMqttSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author  hszhou
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
     * 2025-06-02 14:58:27
     * @param session 会话
     * @param topic 主题
     * @return boolean
     **/
    public boolean checkSubscribePermission(TlMqttSession session,String topic){

        TlAclRequest request = TlAclRequest.builder().ip(session.getIp()).username(session.getUsername())
            .client(session.getClientId()).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkSubscribe(request);
    }

    /**
     * 校验是否有发布权限
     * @author hszhou
     * 2025-06-02 14:58:27
     * @param clientId 客户端id
     * @param username 用户名
     * @param ip ip
     * @param topic 主题
     * @return boolean
     **/
    public boolean checkPublishPermission(String clientId,String username,String ip,String topic) {
        TlAclRequest request = TlAclRequest.builder().ip(ip).username(username)
            .client(clientId).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkPublish(request);
    }
}
