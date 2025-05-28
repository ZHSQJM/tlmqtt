package com.tlmqtt.common.config;

import lombok.Data;
/**
 * @Author: hszhou
 * @Date: 2024/12/31 10:07
 * @Description: 是否开启ssl配置
 */
@Data
public class TlSslProperties {

    private boolean enabled;

    private String certPath;

    private String privatePath;


}
