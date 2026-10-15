package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.entity.AuditLog;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.AuditLogMapper;
import com.orange.farm.util.UserContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 操作审计服务：记录关键管理操作（谁、何时、做了什么）
 */
@Service
public class AuditService {

    @Resource
    private AuditLogMapper auditLogMapper;

    /**
     * 记录一条审计日志（自动带上当前登录用户）
     */
    public void log(String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        try {
            User u = UserContext.get();
            log.setUserId(u.getId());
            log.setUsername(u.getUsername());
            log.setRole(u.getRole());
        } catch (Exception e) {
            log.setUsername("anonymous");
            log.setRole("-");
        }
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    /**
     * 审计日志分页查询（管理端）
     */
    public Page<AuditLog> page(int page, int size, String keyword) {
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("username", keyword)
                    .or().like("action", keyword)
                    .or().like("detail", keyword);
        }
        wrapper.orderByDesc("id");
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
