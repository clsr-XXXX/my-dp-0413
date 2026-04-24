package com.att.config;

import com.att.utils.LoginInterceptor;
import com.att.utils.RefreshTokenInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**")
                .excludePathPatterns(
                        "blog/hot",
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "/voucher/**",
                        "/upload/**"
                ).order(0);

        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "blog/hot",
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "/voucher/**",
                        "/upload/**"
                );

    }
}
