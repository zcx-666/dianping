package com.hmdp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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


    @Test
    void exceptionTest() throws Exception {
        try {
            throw new Exception();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println(123);
        }
    }

    @Test
    void logTest() {
        Student student = Mockito.mock(Student.class);
        System.out.println(student);
        log.info("log info");
        log.error("log error");
        log.debug("log debug321");
        log.trace("log trace123");
    }

    @Test
    void classTest() {
        Object obj = new UserDTO();
        log.info(obj.getClass().getSimpleName());
    }
}
