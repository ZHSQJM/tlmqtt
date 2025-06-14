package com.tlmqtt.auth.acl;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import com.tlmqtt.common.enums.Action;
/**
 * @Author: hszhou
 * @Date: 2025/6/2 13:57
 * @Description: mqtt的权限认证
 */
@Accessors(chain = true)
@Builder
@Data
public class TlAclRequest {

    private String username;

    private String client;

    private String ip;

    private Action action;

    private String topic;
}
