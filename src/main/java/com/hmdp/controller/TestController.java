package com.hmdp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/error")
    public String error() {
        log.info("error");
        return "Error";
    }

    @GetMapping("/status")
    public String status(HttpServletResponse response) {
        log.info("status");
        response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        return "Fuck";
    }
}
