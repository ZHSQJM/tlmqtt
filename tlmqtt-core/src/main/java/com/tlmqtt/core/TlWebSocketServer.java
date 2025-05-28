package com.tlmqtt.core;

import com.tlmqtt.common.config.*;
import com.tlmqtt.core.codec.MqttWebSocketCodec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;


/**
 * @Author: hszhou
 * @Date: 2025/5/16 14:20
 * @Description: websocket的服务启动类
 */
public class TlWebSocketServer extends AbstractTlServer {
    public TlWebSocketServer() {
        super();
        MqttConfiguration mqttConfiguration = new MqttConfiguration();
        TlMqttProperties mqttProperties = mqttConfiguration.getMqttProperties();
        TlPortProperties port = mqttProperties.getPort();
        setPort(port.getWebsocket());
        TlSslProperties sslProperties = mqttProperties.getSsl();
        boolean ssl = sslProperties.isEnabled();
        if (ssl) {
            int websocketPort = port.getSslWebsocket();
            setPort(websocketPort);
            setCertPath(sslProperties.getCertPath());
            setPrivatePath(sslProperties.getPrivatePath());
        }

    }

    @Override
    public void addPipeline(ChannelPipeline pipeline) {
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
    }

}
