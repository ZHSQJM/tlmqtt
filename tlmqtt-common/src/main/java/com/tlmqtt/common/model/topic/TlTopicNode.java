package com.tlmqtt.common.model.topic;

import com.tlmqtt.common.model.entity.TlSubClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hszhou
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TlTopicNode {

    /**节点所在的字符*/
    private String topicChar;
    /**节点类型：普通节点、单层通配符(+)、多层通配符(#)*/
    private NodeType type;
    /**普通子节点（使用Map提升查询效率）*/
    private Map<String, TlTopicNode> childrenMap = new ConcurrentHashMap<>();
    /**通配符子节点 + */
    private TlTopicNode singleWildcardNode;
    /**通配符子节点 # */
    private TlTopicNode multiWildcardNode;
    /**节点保存的客户端信息*/
    private ConcurrentHashMap<String, TlSubClient> clients = new ConcurrentHashMap<>();
    /** 新增父节点引用（用于反向清理空节点）*/
    private TlTopicNode parent;

}

/**
 * @author hszhou
 **/
enum NodeType {
    /**
     * @description: 节点类型  正常节点 + 节点 #节点
     **/
    NORMAL,
    SINGLE_LEVEL_WILDCARD,
    MULTI_LEVEL_WILDCARD
}
