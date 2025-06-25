package com.tlmqtt.common.config;

import lombok.Data;
/**
 * 是否开启ssl配置
 *
 * @author hszhou
 */
@Data
public class TlSslProperties {

    private boolean enabled;

    private String certPath;

    private String privatePath;


}
