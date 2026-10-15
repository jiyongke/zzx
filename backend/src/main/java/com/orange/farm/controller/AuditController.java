package com.orange.farm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.entity.AuditLog;
import com.orange.farm.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 操作审计日志接口（管理端）
 */
@RestController
@RequestMapping("/api/admin")
public class AuditController {

    @Resource
    private AuditService auditService;

    /** 审计日志分页（可按用户名/操作/详情关键字筛选） */
    @GetMapping("/audit/list")
    @RequireRole({"ADMIN"})
    public Result<Page<AuditLog>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(auditService.page(page, size, keyword));
    }
}
