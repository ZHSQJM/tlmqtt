package com.tlmqtt.common.model.payload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * @author hszhou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TlMqttSubAckPayload {

    private int[] codes;

    public static TlMqttSubAckPayload build(int[] codes) {
       return new TlMqttSubAckPayload(codes);
    }

    public static TlMqttSubAckPayload buildFailure() {
        int[] codes = new int[1];
        codes [0] = 0x80;
        return new TlMqttSubAckPayload(codes);
    }

}
