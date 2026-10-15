package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.orange.farm.common.BusinessException;
import com.orange.farm.dto.LoginDTO;
import com.orange.farm.dto.RegisterDTO;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.UserMapper;
import com.orange.farm.util.JwtUtil;
import com.orange.farm.util.Md5Util;
import com.orange.farm.vo.LoginVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("账号已存在");
        }
        String role = dto.getRole();
        if (role == null || role.isEmpty()) {
            role = "CONSUMER";
        }
        if (!"CONSUMER".equals(role) && !"FARMER".equals(role)) {
            throw new BusinessException("仅支持注册消费者或农户账号");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(Md5Util.md5(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isEmpty() ? dto.getUsername() : dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setStatus(1);
        // 农户账号初始为「未提交资质认证」，需实名+资质认证并经管理员审核后方可发布溯源链/上架商品
        user.setAuditStatus("FARMER".equals(role) ? 0 : null);
        userMapper.insert(user);
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (user == null || !user.getPassword().equals(Md5Util.md5(dto.getPassword()))) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user);
    }
}
