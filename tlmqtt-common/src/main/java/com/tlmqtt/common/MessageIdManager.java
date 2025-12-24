package com.tlmqtt.common;

import lombok.extern.slf4j.Slf4j;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
@Slf4j
public class MessageIdManager {

    private static final int MAX_ID = 65535;
    private static final int MIN_ID = 1;

    /**
     * 当前分配到的 ID 位置索引，用于下一次搜索的起点
     */
    private final AtomicInteger currentId = new AtomicInteger(MIN_ID);

    /**
     * 使用 BitSet 追踪正在使用中的 ID
     * 每一个 bit 代表一个 ID 状态，0 为空闲，1 为占用
     * 占用空间：65536 bit ≈ 8 KB，极其轻量
     */
    private final BitSet inUse = new BitSet(MAX_ID + 1);

    /**
     * 获取下一个可用的消息 ID (线程安全)
     * * @return 1-65535 之间的整数
     * @throws RuntimeException 如果 65535 个 ID 全部被占用
     */
    public synchronized int getNextId() {
        int startId = currentId.get();
        int candidate = startId;

        // 循环搜索一个未被占用的 ID
        while (true) {
            // 检查 candidate 是否被占用
            if (!inUse.get(candidate)) {
                // 标记为占用
                inUse.set(candidate);

                // 更新下一次搜索的起点（移动到下一个 ID，实现回转循环）
                int nextStart = (candidate >= MAX_ID) ? MIN_ID : candidate + 1;
                currentId.set(nextStart);

                return candidate;
            }

            // 如果被占用，寻找下一个
            candidate = (candidate >= MAX_ID) ? MIN_ID : candidate + 1;

            // 如果绕了一圈回到起点，说明 ID 耗尽
            if (candidate == startId) {
                log.error("Critical: All 65535 Message IDs are in-flight. Potential leak or extreme load.");
                throw new RuntimeException("No available Message ID (Protocol Limit Reached)");
            }
        }
    }

    /**
     * 释放 ID (线程安全)
     * 当收到 PUBACK, PUBREC (QoS 2 第一阶段), 或 PUBCOMP (QoS 2 第二阶段) 时调用
     * * @param messageId 需要释放的 ID
     */
    public synchronized void releaseId(int messageId) {
        if (messageId < MIN_ID || messageId > MAX_ID) {
            return;
        }

        // 清除占用标记，该 ID 之后可以被重新分配
        if (inUse.get(messageId)) {
            inUse.clear(messageId);
            log.trace("MessageId [{}] released", messageId);
        }
    }

    /**
     * 重置管理器（通常在清理 Session 时使用）
     */
    public synchronized void clear() {
        inUse.clear();
        currentId.set(MIN_ID);
    }
}
