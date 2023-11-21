package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_ATTRIBUTE;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session
        HttpSession session = request.getSession();
        // 获取用户信息
        Object user = session.getAttribute(USER_ATTRIBUTE);
        // user是否存在
        if (user == null) {
            // 拦截请求，返回状态码401
            response.setStatus(401);
            return false;
        }
        // 用户存在，保存用户信息到ThreadLocal
        User u = (User) user;
        UserDTO userDTO = new UserDTO();
        userDTO.setNickName(u.getNickName());
        userDTO.setId(u.getId());
        UserHolder.saveUser(userDTO);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
