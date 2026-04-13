package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    /**
     * 发送验证码
      * @param phone 手机号
      * @param session 会话对象
      * @return 结果对象，包含发送结果信息
     */
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if(!RegexUtils.isCodeInvalid(phone)){
            log.debug("手机号格式错误");
            return Result.fail("手机号格式错误");
        }
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3. 保存验证码到session
        session.setAttribute("code", code);

        // 4.  发送验证码
        log.debug("发送短信验证码成功，手机号: {}, 验证码: {}", phone, code);

        return Result.ok();
    }

    /**
     * 登录功能
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        if(!RegexUtils.isCodeInvalid(phone)){

            return Result.fail("手机号格式错误");
        }
        // 2. 校验验证码        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if(code == null || !code.equals(session.getAttribute("code"))){
            return Result.fail("验证码错误");
        }
        // 3. 查询用户信息
        User user = query().eq("phone", phone).one();
        // 4. 判断用户是否存在
        if(user == null){
            // 5. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        // 6. 保存用户信息到session
        session.setAttribute("user", user);
        // 7. 返回登录结果
        return Result.ok();

    }

    /**
     * 根据手机号创建用户
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(4));
        save(user);
        return user;
    }
}
