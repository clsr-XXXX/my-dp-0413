package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

//        if(userObj == null){
//            response.setStatus(401);
//            return false;
//        }

        // 2. 关键修复：把 User 转为 UserDTO 再存入 ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

//        if (userObj instanceof User user) {
//            // 使用 Hutool 的 BeanUtil 安全转换（推荐，黑马项目常用）
//            userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//        } else if (userObj instanceof UserDTO dto) {
//            userDTO = dto;
//        } else {
//            response.setStatus(401);
//            return false;
//        }

        // 3. 保存到 ThreadLocal
        UserHolder.saveUser(userDTO);

        // 4. 刷新 token 有效期（如果使用了 token 方式，这里需要刷新 token 的过期时间）
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
