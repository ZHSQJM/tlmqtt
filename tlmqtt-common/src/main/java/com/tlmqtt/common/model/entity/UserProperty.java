package com.tlmqtt.common.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author hszhou
 */

@Data
@RequiredArgsConstructor
@Builder
public class UserProperty {
    public final String key;
    public final String value;
}
