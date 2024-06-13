package com.hmdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Configuration
public class studentConfig {
    @Bean(destroyMethod = "destroy", initMethod = "init")
    public static Student getStudent() {
        Student student = new Student();
        student.setId(10L);
        student.setName("Configuration");
        return student;
    }


}
