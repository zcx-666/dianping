package com.hmdp;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
//@Component
public class Student {
    private Long id;
    private String name = "ZCX";

    
    private void destroy() {
        System.out.println(this.name +" 死了");
    }

    private void init() {
        System.out.println(this.name + " 活了");
    }
}