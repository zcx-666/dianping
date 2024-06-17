package com.hmdp.config.kafka;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.User;
import org.apache.kafka.common.serialization.Deserializer;

import static com.hmdp.config.kafka.KafkaConfig.VOUCHER_TOPIC;


public class KafkaDeserializer implements Deserializer<Object> {
    @Override
    public Object deserialize(String topic, byte[] data) {
        String jsonStr = new String(data);
        if (topic.equals(VOUCHER_TOPIC)) {
            return JSONUtil.toBean(jsonStr, User.class);
        }
        return "ERROR TOPIC";
    }
}
