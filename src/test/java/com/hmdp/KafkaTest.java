package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
public class KafkaTest {


    @Resource
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private static final String TOPIC = "topic_input";

    @Test
    void sendMsg() {
        kafkaTemplate.send(TOPIC, "Msg1");
        kafkaTemplate.send(TOPIC, "Msg2");
        kafkaTemplate.send(TOPIC, "Msg3");
        kafkaTemplate.send(TOPIC, "Msg4");
        kafkaTemplate.send(TOPIC, "Msg5");
        kafkaTemplate.send(TOPIC, "Msg6");
    }

    @KafkaListener(id = "webGroup", topics = TOPIC)
    public void listen(String input) {
        log.info("input value: {}", input);
    }

}
