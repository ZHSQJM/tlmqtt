package com.tlmqtt.authptozation.file;

import com.tlmqtt.common.authorization.TlAclRequest;
import com.tlmqtt.common.authorization.TlAuthorizationProvider;
import com.tlmqtt.common.enums.Action;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class FileAuthorizationProvider implements TlAuthorizationProvider {


    private final LocalAclValidator localAclValidator;

    public FileAuthorizationProvider() {
        localAclValidator = new LocalAclValidator("acl.conf");
    }
    @Override
    public boolean checkSubscribePermission(String clientId, String username, String ip, String topic) {

        TlAclRequest request = TlAclRequest.builder().ip(ip).username(username)
            .client(clientId).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkSubscribe(request);
    }

    @Override
    public boolean checkPublishPermission(String clientId, String username, String ip, String topic) {
        TlAclRequest request = TlAclRequest.builder().ip(ip).username(username)
            .client(clientId).action(Action.SUB).topic(topic).build();
        return localAclValidator.checkPublish(request);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public String name() {
        return "FILE";
    }
}
