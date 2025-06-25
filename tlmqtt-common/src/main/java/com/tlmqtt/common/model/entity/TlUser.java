package com.tlmqtt.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本地文件用户信息
 *
 * @author hszhou
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class TlUser {

    /****
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
