package com.tlmqtt.core.jvm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @author hszhou
 */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class Student extends Person{

    private int age;

    public static void main(String[] args) {

        Student student = Student.builder()
            .name("hszhou")
            .age(18)
            .build();

        student.getName();
    }
}

