package com.tlmqtt.bootstrap;

import com.tlmqtt.common.properties.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

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

        try (InputStream certIn = Files.newInputStream(certFile.toPath());
            InputStream keyIn = Files.newInputStream(privateKeyFile.toPath())) {
            return SslContextBuilder.forServer(certIn, keyIn)
                .sslProvider(SslProvider.JDK)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("【TLMQTT】 SSL fail", e);
        }
    }
}
