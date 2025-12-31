package com.tlmqtt.common.properties;

import com.tlmqtt.common.model.entity.TlAuthUser;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 是否开启认证以及本地配置的用户名与密码
 *
 * @author hszhou
 */
@Data
public class TlAuthProperties {

    /** 是否开启认证 */
    private boolean enabled = true;

   private List<TlAuthUser> user = new ArrayList<>();

}
