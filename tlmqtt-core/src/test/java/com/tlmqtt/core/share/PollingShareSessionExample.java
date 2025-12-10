package com.tlmqtt.core.share;

import com.tlmqtt.common.model.TlMqttSession;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮询选择器使用示例
 */
public class PollingShareSessionExample {
    
    // 计数器用于生成不同的会话ID
    private static int sessionIdCounter = 0;
    
    public static void main(String[] args) {
        // 创建轮询选择器
        PollingShareSubscribeClientChoose pollingShareSession = new PollingShareSubscribeClientChoose();
        
        // 模拟创建会话列表
        List<TlMqttSession> sessions = createMockSessions();
        
        // 模拟多次选择操作，观察轮询效果
        System.out.println("轮询选择测试:");
        for (int i = 1; i <= 10; i++) {
            TlMqttSession chosenSession = pollingShareSession.choose(sessions);
            System.out.println("第" + i + "次选择: Session-" + getSessionId(chosenSession));
        }
    }
    
    /**
     * 创建模拟会话列表
     */
    private static List<TlMqttSession> createMockSessions() {
        List<TlMqttSession> sessions = new ArrayList<>();
        
        // 创建3个模拟会话
        for (int i = 1; i <= 3; i++) {
            TlMqttSession session = new TlMqttSession();
            // 注意：这里仅为演示目的，在真实环境中需要正确设置会话属性
            sessions.add(session);
        }
        
        return sessions;
    }
    
    /**
     * 获取会话ID（仅为演示）
     */
    private static int getSessionId(TlMqttSession session) {
        // 在真实环境中，您可以通过session.getClientId()等方式获取会话标识
        // 这里我们简单地使用对象的哈希码作为标识
        return Math.abs(session.hashCode()) % 1000;
    }
}