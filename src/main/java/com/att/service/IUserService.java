package com.att.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.att.dto.LoginFormDTO;
import com.att.dto.Result;
import com.att.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码
     * @return
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     * @param loginForm
     * @param session
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
