1. 数据源： 指定规则作用于哪些topic
2. 筛选条件:  基于消息payload或metadata（如ClientID，Qos）进行过滤
3. 数据转换: 提取，修改或增强数据字段
4. 动作: 处理后的数据去向哪里 如存入mysq，发送kafka，触发webhook，重发布到另一个topic