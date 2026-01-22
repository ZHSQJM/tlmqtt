package com.tlmqtt.common.authentication;

import lombok.Data;

import java.util.Objects;

/**
 * @author zhouhs
 * @version 0.1.0
 * @since 0.1.0
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
