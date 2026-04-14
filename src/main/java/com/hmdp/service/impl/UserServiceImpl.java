package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    /**
     * 发送验证码
      * @param phone 手机号
      * @param session 会话对象
      * @return 结果对象，包含发送结果信息
     */
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            log.debug("手机号格式错误");
            return Result.fail("手机号格式错误");
        }
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3. 保存验证码到session
        // session.setAttribute("code", code);
        // 3. 保存到redis当中
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

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
        if(RegexUtils.isPhoneInvalid(phone)){

            return Result.fail("手机号格式错误");
        }
        // 2. 校验验证码        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
//        if(code == null || !code.equals(session.getAttribute("code"))){
//            return Result.fail("验证码错误");
//        }
        // 2 . 校验验证码（从redis中获取验证码并校验）
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        log.debug("cacheCode: {}, code: {}", cacheCode, code);
        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }
        // 删除验证码，防止重复使用
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);
        // 3. 查询用户信息
        User user = query().eq("phone", phone).one();
        // 4. 判断用户是否存在
        if(user == null){
            // 5. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        // 6. 保存用户信息到session （为了安全，保存 UserDTO 而不是 User）
        // 6. 保存用户信息到redis中
        // 6. 保存用户信息到 Redis（关键修复：把所有 value 转为 String）
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.toBean(user, UserDTO.class);   // 或 BeanUtil.copyProperties(user, UserDTO.class);

// 使用 CopyOptions 把所有字段值强制转为 String（最干净的解决办法）
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)          // 忽略 null 值（icon 可能为空）
                        .setFieldValueEditor((fieldName, fieldValue) -> {
                            if (fieldValue == null) {
                                return null;
                            }
                            return fieldValue.toString();   // Long、String 等全部转成 String
                        })
        );

        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

// 7. 返回 token 给前端
        return Result.ok(token);

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
