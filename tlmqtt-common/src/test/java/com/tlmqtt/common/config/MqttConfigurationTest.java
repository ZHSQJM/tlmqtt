//package com.tlmqtt.common.config;
//
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//import java.util.Arrays;
//import java.util.List;
//
//public class MqttConfigurationTest {
//
//    @Test
//    public void testDefaultValues() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试默认值
//        assertEquals(3600, config.getInt(MqttConfiguration.SESSION_EXPIRY_INTERVAL));
//        assertEquals(200, config.getInt(MqttConfiguration.TOPIC_ALIAS_MAXIMUM));
//        assertEquals(65535, config.getInt(MqttConfiguration.MAXIMUM_PACKET_SIZE));
//        assertEquals(2, config.getInt(MqttConfiguration.MAXIMUM_QOS));
//        assertTrue(config.getBoolean(MqttConfiguration.RETAIN_AVAILABLE));
//        assertTrue(config.getBoolean(MqttConfiguration.WILDCARD_SUBSCRIPTION_AVAILABLE));
//        assertTrue(config.getBoolean(MqttConfiguration.SUBSCRIPTION_IDENTIFIERS_AVAILABLE));
//        assertTrue(config.getBoolean(MqttConfiguration.SHARED_SUBSCRIPTION_AVAILABLE));
//    }
//
//    @Test
//    public void testSetAndGetBoolean() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试设置和获取布尔值
//        config.setBoolean("test.boolean", true);
//        assertTrue(config.getBoolean("test.boolean"));
//
//        config.setBoolean("test.boolean", false);
//        assertFalse(config.getBoolean("test.boolean"));
//    }
//
//    @Test
//    public void testSetAndGetInt() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试设置和获取整数值
//        config.setInt("test.int", 123);
//        assertEquals(123, config.getInt("test.int"));
//
//        config.setInt("test.int", -456);
//        assertEquals(-456, config.getInt("test.int"));
//    }
//
//    @Test
//    public void testSetAndGetStringList() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试设置和获取字符串列表
//        List<String> testList = Arrays.asList("item1", "item2", "item3");
//        config.setStringList("test.list", testList);
//        assertEquals(testList, config.getStringList("test.list"));
//    }
//
//    @Test
//    public void testUpdateStringList() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试更新字符串列表
//        config.updateStringList("test.list", "item1", true); // 添加
//        assertTrue(config.getStringList("test.list").contains("item1"));
//
//        config.updateStringList("test.list", "item2", true); // 添加
//        assertTrue(config.getStringList("test.list").contains("item2"));
//
//        config.updateStringList("test.list", "item1", false); // 删除
//        assertFalse(config.getStringList("test.list").contains("item1"));
//        assertTrue(config.getStringList("test.list").contains("item2"));
//    }
//
//    @Test
//    public void testDefaultValueFallback() {
//        MqttConfiguration config = new MqttConfiguration();
//
//        // 测试默认值回退
//        assertFalse(config.getBoolean("nonexistent.boolean"));
//        assertTrue(config.getBoolean("nonexistent.boolean", true));
//
//        assertEquals(0, config.getInt("nonexistent.int"));
//        assertEquals(999, config.getInt("nonexistent.int", 999));
//
//        assertNotNull(config.getStringList("nonexistent.list"));
//        assertTrue(config.getStringList("nonexistent.list").isEmpty());
//    }
//}