package com.hmdp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;

public class JavaTest {

    private List<Student> typeJSONListToList(List<String> typeListStr) {
        List<Student> res = new ArrayList<>(typeListStr.size());
        for (String s : typeListStr) {
            res.add(JSONUtil.toBean(s, Student.class));
        }
        return res;
    }

    private List<String> typeListToJSON(List<Student> typeList) {
        JSONArray temp = JSONUtil.parseArray(typeList);
        return temp.toList(String.class);
    }

    @Test
    void listJSONTransTest() {
        List<Student> shopTypeList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            shopTypeList.add(new Student().setId((long) i));
        }
        List<String> jsonList = typeListToJSON(shopTypeList);
        List<Student> typeList = typeJSONListToList(jsonList);
    }

    @Test
    void resourceTest() {
        ClassPathResource resource = new ClassPathResource("lua/unlock.lua");
    }
}
