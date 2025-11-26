##### 遗嘱消息的发布
- 服务端检测到了一个I/O错误或者网络故障。
- 客户端在保持连接（Keep Alive）的时间内未能通讯。
- 客户端没有先发送原因码为 0x00 （正常断连）的 DISCONNECT 报文直接关闭了网络连接。
- 服务器没有先发送原因码为 0x00 （正常断连）的 DISCONNECT 报文直接关闭了网络连接。

- 服务器中的遗嘱消息必须要从存储会话状态中移除


mqtt 测试
建立 100 个客户端连接，每 10ms 建立一个连接，每个连接均订阅 testtopic/# 主题，QoS 为 2
./emqtt_bench conn -c 100 -h broker.hivemq.com

建立 100 个客户端连接，每 10ms 建立一个连接，每个连接均订阅 testtopic/# 主题，QoS 为 2
./emqtt_bench sub -c 100 -i 10 -t testtopic/# -q 2 -h broker.hivemq.com

建立 100 个客户端连接，每 10ms 建立一个连接，每个连接 10ms 发布一次消息，每个连接均向 testtopic/${clientid} 主题发布消息，单条消息尺寸为 256 Bytes，消息 QoS 为 2
./emqtt_bench pub -c 100 -i 10 -I 10 -t testtopic/%i -s 256 -q 2 -h broker.hivemq.com


//建立5000个连接
docker run -it emqx/emqtt-bench:latest conn -h 172.28.32.1 -p 18883 -c 5000 -V 4 -u mqtt -P mqtt -i 10

建立 5000 个客户端连接，每 10ms 建立一个连接，每个连接均订阅 testtopic/# 主题，QoS 为 2
docker run -it emqx/emqtt-bench:latest sub -h 172.28.32.1 -p 18883 -c 5000 -i 10 -t  testtopic/#  -q 2 -V 4 -u mqtt -P mqtt
建立 100 个客户端连接，每 10ms 建立一个连接，每个连接 10ms 发布一次消息，每个连接均向 testtopic/${clientid} 主题发布消息，单条消息尺寸为 256 Bytes，消息 QoS 为 2
docker run -it emqx/emqtt-bench:latest pub -h 172.28.32.1 -p 18883 -c 5000 -i 10 -I 10 -t testtopic/%i -s 256  -q 2 -V 4 -u mqtt -P mqtt