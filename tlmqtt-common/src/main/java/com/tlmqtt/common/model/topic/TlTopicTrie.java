package com.tlmqtt.common.model.topic;

import com.tlmqtt.common.model.entity.TlSubClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * 字典树 存储订阅关系
 */
public class TlTopicTrie {

    public final static String SLASH = "/";
    private final static String HASH = "#";
    private final static String PLUS = "+";

    /** 根节点 不保存任何数据 */
    private final TlTopicNode root;

    public TlTopicTrie() {
        root = new TlTopicNode();
    }

    /**
     * 插入一条数据
     * @param topic 主题名称
     * @param client 主题的客户端
     */
    public void insert(String topic, TlSubClient client) {
        // 分割主题为多个部分
        String[] segments = splitTopic(topic);
        TlTopicNode current = root;
        for (int i = 0; i < segments.length; i++) {
            // 遍历主题的每个部分
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);
            // 处理通配符规则
            if (segment.equals(HASH)) {
                // 如果是#通配符 那么必须在最后一个位置
                if (!isLast) {
                    throw new IllegalArgumentException("# must be the last segment");
                }
                handleMultiWildcard(current, client);
                return;
            } else if (segment.equals(PLUS)) {
                // 如果是+通配符
                current = handleSingleWildcard(current);
            } else {
                // 如果是普通节点
                current = handleNormalSegment(current, segment);
            }
        }
        // 等待遍历完成后 这个current就是最后一个节点 将client保存到最后的一个节点
        current.getClients().put(client.getClientId(), client);
    }

    /**
     * 给节点添加通配符节点 节点的通配符节点只可能存在一个#通配符的节点
     * @param parent 父节点 就是#的上一个节点
     * @param client 需要保存的客户端
     */
    private void handleMultiWildcard(TlTopicNode parent, TlSubClient client) {
        // 如果当前节点没有#通配符的子节点
        if (parent.getMultiWildcardNode() == null) {
            // 构造一个新节点
            TlTopicNode node = new TlTopicNode();
            node.setType(NodeType.MULTI_LEVEL_WILDCARD);
            // 设置父节点
            node.setParent(parent);
            parent.setMultiWildcardNode(node);
        }
        // 将客户端添加到#通配符子节点的客户端列表中
        parent.getMultiWildcardNode().getClients().put(client.getClientId(), client);
    }

    /**
     * 处理节点是+通配符
     * @param parent 父节点 也就是上一个节点
     * @return 处理后的节点
     */
    private TlTopicNode handleSingleWildcard(TlTopicNode parent) {
        // 父节点的+通配符节点也只能存在一个
        if (parent.getSingleWildcardNode() == null) {
            // 构造一个新节点
            TlTopicNode node = new TlTopicNode();
            node.setType(NodeType.SINGLE_LEVEL_WILDCARD);
            node.setParent(parent);
            parent.setSingleWildcardNode(node);
        }
        return parent.getSingleWildcardNode();
    }

    /**
     * 处理普通节点
     * @param parent 父节点
     * @param segment 分割后的字符串
     * @return 分割后的字符串所创建的节点
     */
    private TlTopicNode handleNormalSegment(TlTopicNode parent, String segment) {
        // 构建这个节点，然后保存到父节点的普通节点的列表中
        return parent.getChildrenMap().computeIfAbsent(segment, k -> {
            TlTopicNode node = new TlTopicNode();
            node.setParent(parent); // 设置父节点
            return node;
        });
    }

    /**
     * 根据主题和客户端删除订阅信息
     * @param topic 主题名称
     * @param client 客户端信息
     */
    public void remove(String topic, TlSubClient client) {
        if ("".equals(topic) || topic == null) {
            return;
        }
        // 分割主题为多个部分
        String[] segments = topic.split("/");
        // 创建一个栈 为了记录遍历到路线 也就是parent的路线 后续通过这个栈里面的数据进行回溯删除
        Stack<TlTopicNode> pathStack = new Stack<>();
        TlTopicNode current = root;

        // Step 1: 遍历路径，记录节点路径栈
        for (String segment : segments) {
            // 记录父节点路径
            pathStack.push(current);
            // 处理通配符规则
            if (segment.equals(HASH)) {
                current = current.getMultiWildcardNode();
                break; // # 必须是最后一个段
            } else if (segment.equals(PLUS)) {
                current = current.getSingleWildcardNode();
            } else {
                current = current.getChildrenMap().get(segment);
            }
            // 路径不存在
            if (current == null) {
                return;
            }
        }

        // Step 2: 删除客户端（仅叶子节点有效） 这个current就是叶子节点
        if (current != null && !current.getClients().isEmpty()) {
            // 需确保 TlSubClient 正确实现 equals()
            current.getClients().remove(client.getClientId());
            // Step 3: 反向清理空节点
            if (current.getClients().isEmpty()) {
                cleanupEmptyNodes(current, pathStack);
            }
        }
    }

    /**
     * 根据 clientId 删除该客户端订阅的所有主题信息
     * @param clientId 客户端 ID
     */
    public void removeAll(String clientId) {
        removeByClientIdRecursive(root, clientId);
    }

    /**
     * 递归删除指定 clientId 的客户端信息，并清理空节点
     * @param node 当前节点
     * @param clientId 客户端 ID
     */
    private void removeByClientIdRecursive(TlTopicNode node, String clientId) {
        // 删除当前节点中指定 clientId 的客户端信息
        node.getClients().remove(clientId);

        // 递归处理普通子节点
        for (TlTopicNode child : node.getChildrenMap().values()) {
            removeByClientIdRecursive(child, clientId);
        }

        // 递归处理单层通配符子节点
        if (node.getSingleWildcardNode() != null) {
            removeByClientIdRecursive(node.getSingleWildcardNode(), clientId);
        }

        // 递归处理多层通配符子节点
        if (node.getMultiWildcardNode() != null) {
            removeByClientIdRecursive(node.getMultiWildcardNode(), clientId);
        }

        // 清理空节点
        if (node.getClients().isEmpty() && node.getChildrenMap().isEmpty()
            && node.getSingleWildcardNode() == null && node.getMultiWildcardNode() == null) {
            TlTopicNode parent = node.getParent();
            if (parent != null) {
                if (parent.getChildrenMap().containsValue(node)) {
                    parent.getChildrenMap().entrySet().removeIf(entry -> entry.getValue() == node);
                } else if (parent.getSingleWildcardNode() == node) {
                    parent.setSingleWildcardNode(null);
                } else if (parent.getMultiWildcardNode() == node) {
                    parent.setMultiWildcardNode(null);
                }
            }
        }
    }

    /**
     * 从叶子节点向上清理空节点
     * @param node 当前待清理节点
     * @param pathStack 路径栈（从根到当前节点的父节点）
     */
    private void cleanupEmptyNodes(TlTopicNode node, Stack<TlTopicNode> pathStack) {
        TlTopicNode current = node;
        while (!pathStack.isEmpty()) {
            TlTopicNode parent = pathStack.pop();
            // 判断当前节点是否可删除（无客户端、无子节点、无通配符子节点）
            boolean canRemove = current.getClients().isEmpty()
                && current.getChildrenMap().isEmpty()
                && current.getSingleWildcardNode() == null
                && current.getMultiWildcardNode() == null;
            if (!canRemove) {
                break;
            }
            // 从父节点中移除当前节点
            if (parent.getChildrenMap().containsValue(current)) {
                // 普通子节点
                TlTopicNode finalCurrent = current;
                parent.getChildrenMap().entrySet().removeIf(entry -> entry.getValue() == finalCurrent);
            } else if (parent.getSingleWildcardNode() == current) {
                // 单层通配符+子节点
                parent.setSingleWildcardNode(null);
            } else if (parent.getMultiWildcardNode() == current) {
                // 多层通配符#子节点
                parent.setMultiWildcardNode(null);
            }
            // 继续向上检查父节点
            current = parent;
        }
    }

    /**
     * 搜索匹配主题的所有客户端信息
     * @param topic 主题名称
     * @return 匹配的客户端列表
     */
    public List<TlSubClient> search(String topic) {
        List<TlSubClient> result = new ArrayList<>();
        String[] segments = topic.split("/");
        searchRecursive(root, segments, 0, result);
        return result;
    }

    /**
     * 递归搜索匹配主题的客户端信息
     * @param node 当前节点
     * @param segments 主题分割后的部分
     * @param depth 当前遍历的深度
     * @param result 存储匹配客户端的列表
     */
    private void searchRecursive(TlTopicNode node, String[] segments, int depth, List<TlSubClient> result) {
        // 终止条件：已遍历所有段
        if (depth == segments.length) {
            collectAllClients(node, result);
            return;
        }
        String currentSegment = segments[depth];
        // 匹配普通节点
        TlTopicNode child = node.getChildrenMap().get(currentSegment);
        if (child != null) {
            searchRecursive(child, segments, depth + 1, result);
        }
        // 匹配单层通配符+
        if (node.getSingleWildcardNode() != null) {
            searchRecursive(node.getSingleWildcardNode(), segments, depth + 1, result);
        }
        // 匹配多层通配符#
        if (node.getMultiWildcardNode() != null) {
            collectAllClients(node.getMultiWildcardNode(), result);
        }
    }

    /**
     * 收集节点及其子节点的所有客户端信息
     * @param node 当前节点
     * @param result 存储客户端信息的列表
     */
    private void collectAllClients(TlTopicNode node, List<TlSubClient> result) {
        Collection<TlSubClient> clients = node.getClients().values();
        result.addAll(clients);
        // 递归收集所有子节点的客户端（针对多层通配符）
        for (TlTopicNode child : node.getChildrenMap().values()) {
            collectAllClients(child, result);
        }
    }

    /**
     * 分割主题为多个部分
     * @param topic 主题名称
     * @return 分割后的字符串数组
     */
    private String[] splitTopic(String topic) {
        if (topic == null || topic.isEmpty()) return new String[0];
        return topic.split(SLASH);
    }
}
