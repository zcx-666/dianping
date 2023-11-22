package com.hmdp;

import org.junit.jupiter.api.Test;

public class JavaTest {

    void printJob(PrintInfo printInfo) {
        printInfo.print();
    }

    interface PrintInfo {
        void print();
    }

    @Test
    void fun() {
        printJob(() -> System.out.println("We're learning Java 8 fundamentals !"));
    }

}
