package com.tlmqtt.common.properties;

import lombok.Data;

/**
 * 是否开启ssl配置
 *
 * @author hszhou
 */
@Data
public class TlSslProperties {

    private boolean enabled = false;

    private String certPath;

    private String privatePath;


}
