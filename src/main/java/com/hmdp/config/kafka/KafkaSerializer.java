package com.hmdp.config.kafka;

import cn.hutool.json.JSONUtil;
import org.apache.kafka.common.serialization.Serializer;

import java.io.UnsupportedEncodingException;

public class KafkaSerializer implements Serializer<Object> {
    private static final String ENCODING = "UTF8";

    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            return JSONUtil.toJsonStr(data).getBytes(ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
