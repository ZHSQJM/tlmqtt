package com.tlmqtt.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublishMessage {

       /**消息id*/
        private Long messageId;
        /**主题*/
        private String topic;
        /**客户端*/
        private String clientId;
        /**消息内容*/
        private String message;
        /**qos*/
        private int qos;
        /**是否是保留消息*/
        private boolean retain;
        /**是否是重复消息*/
        private boolean dup;

        public static PublishMessage build(Long messageId,String topic, String clientId, String message, int qos, boolean retain, boolean dup){
            return new PublishMessage(messageId,topic,clientId,message,qos,retain,dup);
        }
}
