package com.tlmqtt.auth.acl.local;

import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.Action;
import com.tlmqtt.common.enums.SubjectType;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 读取本地acl文件
 *
 * @author  hszhou
 */
@Slf4j
public class LocalAclFileParse {



    /**
     * 加载配置文件
     * @author hszhou
     * : 2025-06-02 15:47:46
     * @param filePath 配置文件路径
     * @return List<AclRule>
     **/
    List<AclRule> loadRulesWithPriority(String filePath) {
        List<String> lines = readLines(filePath);
        List<AclRule> rules = new ArrayList<>();
        // 文件行号即隐式优先级
        int implicitPriority = 0;
        for (String line : lines) {
            // 解析规则文本
            AclRule rule = parseRule(line);
            rule.setPriority(implicitPriority++);
            rules.add(rule);
        }

        // 显式优先级模式（如果规则中有priority标记）
        return rules.stream()
            .sorted(Comparator.comparingInt(AclRule::getPriority))
            .collect(Collectors.toList());
    }

    private AclRule parseRule(String line){
        String[] aclLine = line.split(Constant.VERTICAL_LINE);
        //user:admin
        String typeAndSubject = aclLine[0];
        //topic: a/b
        String topics = aclLine[1];
        //sub
        String action = aclLine[2].trim();
        //allow
        String acl = aclLine[3].trim();
        AclRule aclRule = new AclRule();

        String[] typeAndSubjectArr= typeAndSubject.split(Constant.COLON);
        String aclTypeArr = typeAndSubjectArr[0];
        if("client".equals(aclTypeArr)){
            aclRule.setSubjectType(SubjectType.CLIENT);
        }else if("ip".equals(aclTypeArr)){
            aclRule.setSubjectType(SubjectType.IP);
        }else if("user".equals(aclTypeArr)){
            aclRule.setSubjectType(SubjectType.USER);
        }

        String[] subjectArr = typeAndSubjectArr[1].split(Constant.COMMA);
        Set<String> subjects = Arrays.stream(subjectArr)
            .map(String::trim)
            .filter(s->!s.isEmpty())
            .collect(Collectors.toSet());
        aclRule.setSubjects(subjects);
        String topicStr = topics.split(Constant.COLON)[1];
        String[] topicsArr = topicStr.split(Constant.COMMA);
        Set<String> topicsSets = Arrays.stream(topicsArr)
            // 去除每个主题两端的空格
            .map(String::trim)
            // 可选：过滤掉空字符串
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        aclRule.setTopics(topicsSets);
            switch (action) {
                case "pub":
                    aclRule.setAction(Action.PUB);
                    break;
                case "sub":
                    aclRule.setAction(Action.SUB);
                    break;
                case "*":
                    aclRule.setAction(Action.ALL);
                    break;
                default:
                    break;
            }
        aclRule.setAllow("allow".equals(acl));
        return aclRule;
    }

    public static List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();
        ClassLoader classLoader = LocalAclFileParse.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(filePath)) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().filter(e->!e.trim().startsWith("#")&&!e.trim().isEmpty())
                    .collect(Collectors.toList());
            }
        }
        catch (IOException e) {
            log.error("read acl file fail：{}", e.getMessage());
            return lines;
        }

    }

}
