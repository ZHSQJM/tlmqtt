package com.tlmqtt.source.base;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlmqtt.common.sink.DataSink;
import com.tlmqtt.common.sink.SinkType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * 用于保存source的Item信息
 **/

public class DefaultSourceServiceImpl implements SourceService {

    private final Cache<SinkType, List<DataSink>> cache = Caffeine.newBuilder()
        .build();

    @Override
    public HashMap<SinkType, List<DataSink>> init() {

        return null;
    }

    @Override
    public void add(DataSink sourceBean) {

//        cache.asMap().compute(sourceBean.getSinkType(), (key, existingList) -> {
//            if (existingList == null) {
//                // 如果不存在，创建一个新的可变列表
//                List<DataSink> newList = new ArrayList<>();
//                newList.add(sourceBean);
//                return newList;
//            } else {
//                // 如果存在，直接追加（注意：前提是缓存里的 List 必须是可变的）
//                existingList.add(sourceBean);
//                return existingList;
//            }
//        });
    }

    @Override
    public void remove(DataSink sourceBean) {
//        SinkType type = sourceBean.getSinkType();
//        // 使用 asMap().computeIfPresent 确保原子性地更新
//        cache.asMap().computeIfPresent(type, (key, existingList) -> {
//            existingList.removeIf(item -> item.getId().equals(sourceBean.getId()));
//            // 如果删除后列表为空，可以返回 null 将该 Key 从缓存中彻底移除
//            return existingList.isEmpty() ? null : existingList;
//        });
    }

    @Override
    public List<? extends DataSink> listBySourceType(SinkType sinkType) {
        List<DataSink> list = cache.getIfPresent(sinkType);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    @Override
    public List<DataSink> listByPage(SinkType sinkType, int pageNum, int pageSize) {
        List<DataSink> fullList = cache.getIfPresent(sinkType);

        if (fullList == null || fullList.isEmpty()) {
            return Collections.emptyList();
        }

        return fullList.stream()
            // 跳过前面的数据
            .skip((long) (pageNum - 1) * pageSize)
            // 限制返回数量
            .limit(pageSize)
            .collect(Collectors.toList());
    }
}

