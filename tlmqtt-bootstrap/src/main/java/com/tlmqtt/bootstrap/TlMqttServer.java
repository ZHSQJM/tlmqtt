package com.tlmqtt.bootstrap;

import com.tlmqtt.common.properties.TlChannelProperties;
import com.tlmqtt.common.properties.TlMqttServerProperties;
import com.tlmqtt.core.codec.MqttWebSocketCodec;
import com.tlmqtt.core.codec.TlMqttMessageCodec;
import com.tlmqtt.core.codec.decoder.TlMqttConnectDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttDisConnectDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttHeartBeatDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttPubAckDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttPubCompDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttPubRecDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttPubRelDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttPublishDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttSubscribeDecoder;
import com.tlmqtt.core.codec.decoder.TlMqttUnSubscribeDecoder;
import com.tlmqtt.core.codec.encoder.TlMqttConnAckEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttDisconnectEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttHeaderBeatAckEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttPubAckEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttPubCompEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttPubRecEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttPubRelEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttPublishEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttSubAckEncoder;
import com.tlmqtt.core.codec.encoder.TlMqttUnSubAckEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;


/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public  class  TlMqttServer  {

    private final TlMqttServerProperties properties;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;

    private MqttComponentContainer container;
    private DynamicTrafficShaper trafficShaper;
    private SslContext sslContext;
    private ShutDownGracefully shutdownHook;



    private final ChannelHandler[] mqttEncoders = new ChannelHandler[]{
        new TlMqttConnAckEncoder(), new TlMqttHeaderBeatAckEncoder(), new TlMqttPubAckEncoder(),
        new TlMqttPubCompEncoder(), new TlMqttPublishEncoder(), new TlMqttPubRecEncoder(),
        new TlMqttPubRelEncoder(), new TlMqttSubAckEncoder(), new TlMqttUnSubAckEncoder(),
        new TlMqttDisconnectEncoder()
    };

    public TlMqttServer(TlMqttServerProperties properties) {
        this.properties = properties;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
    }

    public synchronized void setup(MqttComponentContainer container) {
        this.container = container;
        this.container.initHandlers();

        TlChannelProperties cp = properties.getChannel();
        this.trafficShaper = new DynamicTrafficShaper(workerGroup, cp.getWriteLimit(), cp.getReadLimit(), cp.getCheckInterval(), cp.getMaxTime());

        if (properties.getSsl().isEnabled()) {
            this.sslContext = SslContextUtil.buildServerSslContext(properties.getSsl());
        }

        this.shutdownHook = new ShutDownGracefully(bossGroup, workerGroup, container.getExecutorService());
    }

    public void startSocket(int port) {
        ServerBootstrap b = createBootstrap(new MqttProtocolInitializer(false));
        bind(b, port, "MQTT-TCP");
    }

    public void startWebsocket(int port) {
        ServerBootstrap b = createBootstrap(new MqttProtocolInitializer(true));
        bind(b, port, "MQTT-WebSocket");
    }

    private ServerBootstrap createBootstrap(ChannelHandler initializer) {
        return new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(properties.getChannel().getLowWaterMark(), properties.getChannel().getHighWaterMark()))
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(initializer);
    }

    private void bind(ServerBootstrap b, int port, String name) {
        try {
            ChannelFuture f = b.bind(port).sync();
            log.debug("{} Server started on port: {}", name, port);
            shutdownHook.registerShutdownHook(f.channel());
        } catch (Exception e) {
            log.error("{} Server bind failed on port: {}", name, port, e);
        }
    }

    /**
     * Pipeline 初始化器
     */
    private class MqttProtocolInitializer extends ChannelInitializer<SocketChannel> {
        private final boolean isWs;
        MqttProtocolInitializer(boolean isWs) { this.isWs = isWs; }

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline cp = ch.pipeline();

            // 1. SSL 层
            if (sslContext != null){
                cp.addLast( sslContext.newHandler(ch.alloc()));
            }

            // 2. 流量整形层 (动态调整)
            cp.addLast("traffic", trafficShaper.getHandler());

            // 3. 协议拆包层
            if (isWs) {
                cp.addLast(new HttpServerCodec());
                cp.addLast(new HttpObjectAggregator(65536));
                cp.addLast(new HttpContentCompressor());
                cp.addLast(new WebSocketServerProtocolHandler("/mqtt", "mqtt, mqttv3.1, mqttv3.1.1", true, 65536));
                cp.addLast(new MqttWebSocketCodec());
            }


            // 3. 【核心修改】每次 new 一个新的解码器实例
            // 传入的解码组件（Decoder）本身是无状态的单例，但 Codec 包装类必须是多例
            // 编解码组件 (通常是无状态的，可复用)

            TlMqttMessageCodec mqttCodec = new TlMqttMessageCodec(
                new TlMqttConnectDecoder(container.getMqttConfiguration()), new TlMqttDisConnectDecoder(container.getMqttConfiguration()), new TlMqttHeartBeatDecoder(container.getMqttConfiguration()),
                new TlMqttPubAckDecoder(container.getMqttConfiguration()), new TlMqttPubCompDecoder(container.getMqttConfiguration()), new TlMqttPublishDecoder(container.getMqttConfiguration()),
                new TlMqttPubRecDecoder(container.getMqttConfiguration()), new TlMqttPubRelDecoder(container.getMqttConfiguration()), new TlMqttSubscribeDecoder(container.getMqttConfiguration()),
                new TlMqttUnSubscribeDecoder(container.getMqttConfiguration()));
            // 4. MQTT 编解码层
            cp.addLast(mqttCodec);
            cp.addLast(mqttEncoders);

            // 5. 业务逻辑层 (从 Container 获取初始化的单例 Handler)
            cp.addLast(container.getConnectHandler(), container.getDisconnectHandler(),
                container.getHeartBeatHandler(), container.getPubAckHandler(),
                container.getPubCompHandler(), container.getPublishHandler(),
                container.getPubRecHandler(), container.getPubRelHandler(),
                container.getSubscribeHandler(), container.getUnSubscribeHandler());

            // 6. 异常处理
            cp.addLast(container.getExceptionHandler());
        }
    }
}