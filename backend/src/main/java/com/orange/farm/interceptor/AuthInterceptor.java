package com.orange.farm.interceptor;

import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.common.BusinessException;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.UserMapper;
import com.orange.farm.util.JwtUtil;
import com.orange.farm.util.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * 登录与角色权限拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 无需登录的公开路径 */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/category/list",
            "/api/product/list",
            "/api/product/hot",
            "/api/product/detail/*",
            "/api/trace/list",
            "/api/trace/batch/*",
            "/api/trace/query"
    };

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();

        // 公开路径直接放行
        for (String pattern : PUBLIC_PATHS) {
            if (matcher.match(pattern, uri)) {
                // /api/trace/batch/* 仅 GET（公开查看详情）放行；DELETE 删除批次必须登录鉴权
                if ("/api/trace/batch/*".equals(pattern)
                        && !"GET".equalsIgnoreCase(request.getMethod())) {
                    continue;
                }
                return true;
            }
        }

        // 校验 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = token == null ? null : jwtUtil.parseToken(token);
        if (claims == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }

        Long userId = Long.valueOf(claims.get("userId").toString());
        String role = claims.get("role").toString();
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(401, "账号不存在或已被禁用");
        }
        UserContext.set(user);

        // 角色校验
        if (handler instanceof HandlerMethod) {
            RequireRole requireRole = ((HandlerMethod) handler).getMethodAnnotation(RequireRole.class);
            if (requireRole != null && !Arrays.asList(requireRole.value()).contains(role)) {
                throw new BusinessException(403, "无权限执行该操作");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
