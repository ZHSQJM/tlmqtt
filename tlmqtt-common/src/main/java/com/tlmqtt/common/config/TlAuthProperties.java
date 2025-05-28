package com.tlmqtt.common.config;

import com.tlmqtt.common.model.entity.TlUser;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hszhou
 * @Date: 2025/2/8 9:52
 * @Description: 是否开启认证以及本地配置的用户名与密码
 */
@Data
public class TlAuthProperties {

    private boolean enabled;

    private List<TlUser> user = new ArrayList<>();


}
