package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JavaTest {
    @Data
    @Accessors(chain = true)
    class Student {
        private Long id;
        private String name = "ZCX";

    }


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
}
