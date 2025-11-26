package com.tlmqtt.common.model.payload;
import lombok.*;

/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TlMqttSubAckPayload {

    private int[] codes;

}
