package com.tlmqtt.auth.acl.local;

import com.tlmqtt.auth.acl.TlAclRequest;
import com.tlmqtt.common.Constant;
import com.tlmqtt.common.enums.Action;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: hszhou
 * @Date: 2025/5/30 15:23
 * @Description: 必须描述类做什么事情, 实现什么功能
 */
@Slf4j
public class LocalAclValidator {

    private final List<AclRule> subRules = new ArrayList<>();

    private final List<AclRule> pubRules = new ArrayList<>();

    public LocalAclValidator(String path) {
        LocalAclFileParse parse = new LocalAclFileParse();
        List<AclRule> aclRules = parse.loadRulesWithPriority(path);
        Map<Action, List<AclRule>> map = aclRules.stream().collect(Collectors.groupingBy(AclRule::getAction));
        if(map.get(Action.PUB)!=null){
            this.pubRules.addAll(map.get(Action.PUB));
        }
        if(map.get(Action.SUB)!=null){
            this.subRules.addAll(map.get(Action.SUB));
        }
        List<AclRule> allRule = map.get(Action.ALL);
         if (allRule != null && !allRule.isEmpty()) {
            this.pubRules.addAll(allRule);
            this.subRules.addAll(allRule);
        }
    }

    /**
     * 是否能够订阅主题
     *
     * @param request 请求
     * @return boolean
     * @author hszhou
     * @datetime: 2025-06-02 15:54:44
     **/
    public boolean checkSubscribe(TlAclRequest request) {
        for (AclRule rule : subRules) {
            // 首次匹配立即返回
            if (matchesSubscribeRule(request, rule)) {
                return rule.isAllow();
            }
        }
        // 无匹配时 不能订阅
        return false;
    }

    public boolean checkPublish(TlAclRequest request) {
        for (AclRule rule : pubRules) {
            // 首次匹配立即返回
            if (matchesSubscribeRule(request, rule)) {
                return rule.isAllow();
            }
        }
        // 无匹配时 不能发布
        return false;
    }

    private boolean matchesSubscribeRule(TlAclRequest request, AclRule rule) {
        return switch (rule.getSubjectType()) {
            case USER ->( rule.getSubjects().contains(request.getUsername()) || rule.getSubjects().contains("*")) && matchTopic(request.getTopic().trim(), rule);
            case CLIENT -> (rule.getSubjects().contains(request.getClient())|| rule.getSubjects().contains("*")) && matchTopic(request.getTopic().trim(), rule);
            case IP -> (rule.getSubjects().contains(request.getIp())|| rule.getSubjects().contains("*")) && matchTopic(request.getTopic().trim(), rule);

        };
    }

    /**
     * 是否配到了主题
     *
     * @param topic 订阅或者发布的主题
     * @param rule acl 的默认规则
     * @return boolean
     * @author hszhou
     * @datetime: 2025-06-03 09:57:25
     **/
    public boolean matchTopic(String topic, AclRule rule) {
        Set<String> topics = rule.getTopics();
        return topics.stream().anyMatch(e -> matchTopic( topic,e));
    }

    /**
     * 发布订阅的主题与规则主题是否匹配
     *
     * @param topic 发布订阅的主题
     * @param aclTopic acl主题
     * @return boolean
     * @author hszhou
     * @datetime: 2025-06-03 10:06:01
     **/
    private boolean matchTopic(String topic, String aclTopic) {
        if (aclTopic.equals(Constant.ASTERISK) || aclTopic.equals(Constant.TOPIC_SPLITTER)) {
            return true;
        }
        String[] topicArr = topic.split(Constant.TOPIC_SPLITTER_SPLITTER);
        String[] split = aclTopic.split(Constant.TOPIC_SPLITTER_SPLITTER);
        for (int i = 0; i < split.length; i++) {

            if (i == split.length - 1 && split[i].equals(Constant.TOPIC_SPLITTER)) {
                return true;
            }
            if (i >= topicArr.length) {
                return false;
            }
            if (split[i].equals(Constant.TOPIC_WILDCARD)) {
                continue;
            }
            if (!topicArr[i].equals(split[i])) {
                return false;
            }
        }

        return topicArr.length == split.length;
    }

}
