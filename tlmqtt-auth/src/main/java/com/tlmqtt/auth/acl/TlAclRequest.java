package com.tlmqtt.auth.acl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import com.tlmqtt.common.enums.Action;
/**
 * @author  hszhou
 */
@Accessors(chain = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlAclRequest {

    private String username;

    private String client;

    private String ip;

    private Action action;

    private String topic;
}
