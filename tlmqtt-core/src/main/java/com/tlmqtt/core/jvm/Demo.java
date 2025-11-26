package com.tlmqtt.core.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取jvm相关信息
 *
 * @author hszhou
 */
public class Demo {


//    public String getJvmStats() throws Exception {
//        Runtime runtime = Runtime.getRuntime();
//        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
//        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
//        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
//
//        Map<String, Object> stats = new HashMap<>();
//        stats.put("jvmMemory", Map.of(
//            "totalMemory", runtime.totalMemory(),
//            "freeMemory", runtime.freeMemory(),
//            "maxMemory", runtime.maxMemory(),
//            "heapUsage", memoryBean.getHeapMemoryUsage(),
//            "nonHeapUsage", memoryBean.getNonHeapMemoryUsage()
//        ));
//        stats.put("threadInfo", Map.of(
//            "liveThreads", threadBean.getThreadCount(),
//            "peakThreads", threadBean.getPeakThreadCount()
//        ));
//        stats.put("osInfo", Map.of(
//            "cpuCores", osBean.getAvailableProcessors(),
//            "systemLoad", osBean.getSystemLoadAverage()
//        ));
//
//        return new ObjectMapper().writeValueAsString(stats);
//    }
}
