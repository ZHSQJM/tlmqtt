package com.tlmqtt.bootstrap;

import com.tlmqtt.common.properties.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class SslContextUtil {

    private SslContextUtil() {}

    /**
     * 构建服务端SSL上下文
     * @param sslProperties 服务配置
     * @return SslContext
     */
    public static SslContext buildServerSslContext(TlSslProperties sslProperties) {
        if (!sslProperties.isEnabled()) {
            return null;
        }
        // 校验证书路径
        if (sslProperties.getCertPath() == null || sslProperties.getCertPath().isEmpty()) {
            throw new IllegalArgumentException("【TLMQTT】 SSL cert path is null");

        }
        if (sslProperties.getPrivatePath() == null || sslProperties.getPrivatePath().isEmpty()) {
            throw new IllegalArgumentException("【TLMQTT】 SSL private path is null");
        }
        File certFile = new File(sslProperties.getCertPath());
        File privateKeyFile = new File(sslProperties.getPrivatePath());

        try {
            return SslContextBuilder.forServer(certFile, privateKeyFile)
                .sslProvider(SslProvider.JDK)
                // 如果追求性能且环境支持，建议使用 SslProvider.OPENSSL
                .build();
        } catch (Exception e) {
            throw new RuntimeException("【TLMQTT】构建 SSL 上下文失败，请检查证书文件格式和证书链", e);
        }
    }
}
