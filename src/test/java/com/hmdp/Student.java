package com.hmdp;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Data
@Accessors(chain = true)
@Component
class Student {
    private Long id;
    private String name = "ZCX";
}