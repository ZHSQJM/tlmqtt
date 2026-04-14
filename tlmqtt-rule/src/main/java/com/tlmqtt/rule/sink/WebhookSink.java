package com.tlmqtt.rule.sink;

import java.util.Map;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 **/
public class WebhookSink implements ActionSink{
    @Override
    public String getName() {
        return "";
    }

    @Override
    public void process(Map<String, Object> data, Map<String, Object> params) {

    }
}
