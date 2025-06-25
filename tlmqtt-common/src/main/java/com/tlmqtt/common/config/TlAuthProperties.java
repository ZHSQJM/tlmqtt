package com.tlmqtt.common.config;

import com.tlmqtt.common.model.entity.TlUser;
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

    private boolean enabled;

    private List<TlUser> user = new ArrayList<>();


}
