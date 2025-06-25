package com.tlmqtt.auth.acl.local;


import com.tlmqtt.common.enums.Action;
import com.tlmqtt.common.enums.SubjectType;
import lombok.Data;

import java.util.Set;

/**
 * @author  hszhou
 */
@Data
public class AclRule {




    private SubjectType subjectType;
    /**如 ["admin", "watson"]*/
    private Set<String> subjects;
    /**如 ["a/b", "$SYS/#"]*/
    private Set<String> topics;
    /** PUB, SUB*/
    private Action action;
    /**true=allow, false=deny*/
    private boolean allow;
    /**显式优先级*/
    private int priority;
}
