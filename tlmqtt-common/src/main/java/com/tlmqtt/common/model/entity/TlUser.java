package com.tlmqtt.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hszhou
 * @Date: 2025/5/15 13:33
 * @Description: 本地文件用户信息
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
