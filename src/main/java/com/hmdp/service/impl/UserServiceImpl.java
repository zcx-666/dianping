package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 无效手机号
            return Result.fail("无效手机号");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 保存验证码
        session.setAttribute(CODE_ATTRIBUTE, code);
        // 发送验证码
        log.debug("向{}发送短信验证码：{}", phone, code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 无效手机号
            return Result.fail("无效手机号");
        }
        // 校验验证码
        Object cacheCode = session.getAttribute(CODE_ATTRIBUTE);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 验证码不一致
            return Result.fail("验证码错误");
        }
        // 验证码一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 用户是否存在
        if (user == null) {
            // 用户不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }
        // 保存用户信息到session
        session.setAttribute(USER_ATTRIBUTE, user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        // 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        // 保存用户
        save(user);
        return user;
    }
}
