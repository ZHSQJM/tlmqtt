package com.tlmqtt.common.authentication;

import lombok.Data;
import java.util.Objects;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
 * @description 认证的方式 如mysql,http 有可能有多个 比如mysql的认证有2个 但是都是属于mysql的认证处理器的
 **/

@Data
public class TlAuthenticationSubject {

    public Long id;

    public AuthenticationType authenticationType;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        TlAuthenticationSubject that = (TlAuthenticationSubject) object;
        return Objects.equals(id, that.id) && authenticationType == that.authenticationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authenticationType);
    }
}
