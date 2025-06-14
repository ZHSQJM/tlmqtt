package com.tlmqtt.core.server;

import com.tlmqtt.auth.AuthenticationManager;
import com.tlmqtt.auth.acl.AclManager;
import com.tlmqtt.bridge.TlBridgeManager;
import com.tlmqtt.common.config.*;
import com.tlmqtt.common.model.entity.TlUser;
import com.tlmqtt.core.codec.MqttWebSocketCodec;
import com.tlmqtt.core.manager.TlStoreManager;
import com.tlmqtt.core.manager.ChannelManager;
import com.tlmqtt.core.codec.TlMqttMessageCodec;
import com.tlmqtt.core.codec.decoder.*;
import com.tlmqtt.core.codec.encoder.*;
import com.tlmqtt.core.handler.*;
import com.tlmqtt.core.manager.RetryManager;
import com.tlmqtt.core.message.TlMessageService;
import com.tlmqtt.store.service.*;
import com.tlmqtt.store.service.impl.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @Author hszhou
 * @Date 2025/5/13 16:34
 * @Description 服务启动抽象类
 */
@Slf4j
public class TlServer {

    private final NioEventLoopGroup bossGroup;

    private final NioEventLoopGroup workerGroup;

    private final TlMqttConnectDecoder connectDecoder;

    private final TlMqttDisConnectDecoder disConnectDecoder;

    private final TlMqttHeartBeatDecoder heartBeatDecoder;

    private final TlMqttPubAckDecoder pubAckDecoder;

    private final TlMqttPubCompDecoder pubCompDecoder;

    private final TlMqttPublishDecoder publishDecoder;

    private final TlMqttPubRecDecoder pubRecDecoder;

    private final TlMqttPubRelDecoder pubRelDecoder;

    private final TlMqttSubscribeDecoder subscribeDecoder;

    private final TlMqttUnSubscribeDecoder unSubscribeDecoder;

    private final TlMqttConnackEncoder connackEncoder;

    private final TlMqttHeaderBeatEncoder headerBeatEncoder;

    private final TlMqttPubAckEncoder pubAckEncoder;

    private final TlMqttPubCompEncoder pubCompEncoder;

    private final TlMqttPublishEncoder publishEncoder;

    private final TlMqttPubRecEncoder pubRecEncoder;

    private final TlMqttPubRelEncoder pubRelEncoder;

    private final TlMqttSubAckEncoder subAckEncoder;

    private final TlMqttUnSubAckEncoder unSubAckEncoder;

    private final TlHeartbeatEventTriggered heartbeatEventTriggered;

    private final TlConnectEventHandler connectEventHandler;

    private final TlDisconnectEventHandler disconnectEventHandler;

    private final TlHeartBeatEventHandler heartBeatEventHandler;

    private final TlPubAckEventHandler pubAckEventHandler;

    private final TlPubCompEventHandler pubCompEventHandler;

    private final TlPublishEventHandler publishEventHandler;

    private final TlPubRecEventHandler pubRecEventHandler;

    private final TlPubRelEventHandler pubRelEventHandler;

    private final TlSubscribeEventHandler subscribeEventHandler;

    private final TlUnSubscribeEventHandler unSubscribeEventHandler;

    private final TlExceptionHandler exceptionHandler;

    @Getter
    private final TlStoreManager storeManager;
    @Getter
    private final TlBridgeManager bridgeManager;
    @Getter
    private final RetryManager retryManager;
    @Getter
    private final AuthenticationManager authenticationManager;

    private SslContext sslContext;

    @Setter
    private boolean ssl;
    @Setter
    private String certPath;

    private String privatePath;


    public TlServer() {

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        connectDecoder = new TlMqttConnectDecoder();
        disConnectDecoder = new TlMqttDisConnectDecoder();
        heartBeatDecoder = new TlMqttHeartBeatDecoder();
        pubAckDecoder = new TlMqttPubAckDecoder();
        pubCompDecoder = new TlMqttPubCompDecoder();
        publishDecoder = new TlMqttPublishDecoder();
        pubRelDecoder = new TlMqttPubRelDecoder();
        pubRecDecoder = new TlMqttPubRecDecoder();
        subscribeDecoder = new TlMqttSubscribeDecoder();
        unSubscribeDecoder = new TlMqttUnSubscribeDecoder();
        connackEncoder = new TlMqttConnackEncoder();
        headerBeatEncoder = new TlMqttHeaderBeatEncoder();
        pubAckEncoder = new TlMqttPubAckEncoder();
        pubCompEncoder = new TlMqttPubCompEncoder();
        publishEncoder = new TlMqttPublishEncoder();
        pubRecEncoder = new TlMqttPubRecEncoder();
        pubRelEncoder = new TlMqttPubRelEncoder();
        subAckEncoder = new TlMqttSubAckEncoder();
        unSubAckEncoder = new TlMqttUnSubAckEncoder();
        MqttConfiguration mqttConfiguration = new MqttConfiguration();
        TlMqttProperties mqttProperties = mqttConfiguration.getMqttProperties();
        TlSessionProperties session = mqttProperties.getSession();
        int delay = session.getDelay();
        TlAuthProperties auth = mqttProperties.getAuth();
        boolean enabled = auth.isEnabled();
        List<TlUser> user = auth.getUser();

        PublishService publishService = new DefaultPublishServiceImpl();
        PubrelService pubrelService = new DefaultPubrelServiceImpl();
        RetainService retainService = new DefaultRetainServiceImpl();
        SessionService sessionService = new DefaultSessionServiceImpl();
        SubscriptionService subscriptionService = new DefaultSubscriptionServiceImpl(sessionService);
        ChannelManager channelManager = new ChannelManager();
        AclManager aclManager = new AclManager();
        retryManager = new RetryManager(channelManager, delay);
        storeManager = new TlStoreManager(sessionService, subscriptionService, publishService, pubrelService, retainService);
        TlMessageService messageService = new TlMessageService(storeManager, channelManager,retryManager);
        bridgeManager = new TlBridgeManager();
        authenticationManager = new AuthenticationManager(enabled);
        authenticationManager.addFixUsers(user);

        heartbeatEventTriggered = new TlHeartbeatEventTriggered();
        exceptionHandler = new TlExceptionHandler(storeManager,channelManager,messageService);
        connectEventHandler = new TlConnectEventHandler(storeManager,channelManager, authenticationManager,retryManager);
        disconnectEventHandler = new TlDisconnectEventHandler();
        heartBeatEventHandler = new TlHeartBeatEventHandler();
        pubAckEventHandler = new TlPubAckEventHandler(storeManager, retryManager);
        pubCompEventHandler = new TlPubCompEventHandler(storeManager, retryManager);
        publishEventHandler = new TlPublishEventHandler(storeManager, aclManager, bridgeManager, messageService);
        pubRecEventHandler = new TlPubRecEventHandler(storeManager, retryManager);
        pubRelEventHandler = new TlPubRelEventHandler(messageService);
        subscribeEventHandler = new TlSubscribeEventHandler(storeManager, aclManager);
        unSubscribeEventHandler = new TlUnSubscribeEventHandler(storeManager);

    }

    public void setPrivatePath(String privatePath) {
        this.privatePath = privatePath;
        sslContext = sslContext();
    }

    private void initSsl() {
        if (ssl) {
            sslContext = sslContext();
        }
    }

    public void startSocket(int port) {

        ServerBootstrap bootstrap = new ServerBootstrap();
        initSsl();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (ssl) {
                        pipeline.addFirst("ssl", sslContext.newHandler(ch.alloc()));
                    }
                    pipeline.addLast(new TlMqttMessageCodec(connectDecoder, disConnectDecoder, heartBeatDecoder, pubAckDecoder,pubCompDecoder, publishDecoder, pubRecDecoder, pubRelDecoder, subscribeDecoder, unSubscribeDecoder))
                            .addLast(connackEncoder, headerBeatEncoder, pubAckEncoder, pubCompEncoder, publishEncoder, pubRecEncoder, pubRelEncoder, subAckEncoder, unSubAckEncoder)
                            .addLast(connectEventHandler,disconnectEventHandler,heartBeatEventHandler,
                                pubAckEventHandler,pubCompEventHandler,publishEventHandler,pubRecEventHandler,pubRelEventHandler,subscribeEventHandler,unSubscribeEventHandler)
                            .addLast(heartbeatEventTriggered,exceptionHandler);
                }
            });
        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            log.info("mqtt server start success on port【{}】", port);
            channelFuture.channel().closeFuture().sync();
        }
        catch (Exception e) {
            log.error("mqtt server start error", e);
        }
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void startWebsocket(int port) {

        ServerBootstrap bootstrap = new ServerBootstrap();
        initSsl();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (ssl) {
                        pipeline.addFirst("ssl", sslContext.newHandler(ch.alloc()));
                    }
                    addPipeline(pipeline);
                    pipeline.addLast(new TlMqttMessageCodec(connectDecoder, disConnectDecoder, heartBeatDecoder, pubAckDecoder,pubCompDecoder, publishDecoder, pubRecDecoder, pubRelDecoder, subscribeDecoder, unSubscribeDecoder))
                        .addLast(connackEncoder, headerBeatEncoder, pubAckEncoder, pubCompEncoder, publishEncoder, pubRecEncoder, pubRelEncoder, subAckEncoder, unSubAckEncoder)
                        .addLast(connectEventHandler,disconnectEventHandler,heartBeatEventHandler,
                            pubAckEventHandler,pubCompEventHandler,publishEventHandler,pubRecEventHandler,pubRelEventHandler,subscribeEventHandler,unSubscribeEventHandler)
                        .addLast(heartbeatEventTriggered,exceptionHandler);
                }
            });
        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            log.info("websocket server start success on port【{}】", port);
            channelFuture.channel().closeFuture().sync();
        }
        catch (Exception e) {
            log.error("websocket server start error", e);
        }
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 添加管道
     *
     * @param pipeline 添加管道
     */
    public  void addPipeline(ChannelPipeline pipeline){
        // Netty提供的心跳检测
        pipeline.addFirst("idle", new IdleStateHandler(0, 0, 10));
        // 将请求和应答消息编码或解码为HTTP消息
        pipeline.addLast("http-codec", new HttpServerCodec());
        // 将HTTP消息的多个部分合成一条完整的HTTP消息
        pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
        // 将HTTP消息进行压缩编码
        pipeline.addLast("compressor ", new HttpContentCompressor());
        pipeline.addLast("protocol", new WebSocketServerProtocolHandler("/mqtt", "mqtt,mqttv3.1,mqttv3.1.1", true, 65536));
        pipeline.addLast("mqttWebSocket", new MqttWebSocketCodec());
    };

    /**
     * 构建证书
     *
     * @return SslContext
     */
    public SslContext sslContext() {

        if (certPath == null || certPath.isEmpty()) {
            throw new IllegalArgumentException("请配置证书路径");
        }
        File certResource = new File(certPath);
        if (privatePath == null || privatePath.isEmpty()) {
            throw new IllegalArgumentException("请配置私钥路径");
        }

        File keyResource = new File(privatePath);

        try (InputStream certStream = new FileInputStream(certResource);
            InputStream keyStream = new FileInputStream(keyResource)) {

            // 使用 SslContextBuilder 创建 SslContext，并传递 InputStream
            return SslContextBuilder.forServer(certStream, keyStream)
                // 使用 OpenSSL 提供更好的性能 目前使用的是jdk自带的
                .sslProvider(SslProvider.JDK).build();
        }
        catch (Exception e) {
            log.error("build sslContext exception", e);
            throw new IllegalArgumentException("构建sslContext异常", e);
        }
    }

}
