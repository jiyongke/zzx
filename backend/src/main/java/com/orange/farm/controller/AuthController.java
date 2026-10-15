package com.orange.farm.controller;

import com.orange.farm.common.Result;
import com.orange.farm.dto.LoginDTO;
import com.orange.farm.dto.RegisterDTO;
import com.orange.farm.entity.User;
import com.orange.farm.service.AuthService;
import com.orange.farm.service.UserService;
import com.orange.farm.vo.LoginVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private UserService userService;

    /** 注册（默认消费者） */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.profile());
    }
}
