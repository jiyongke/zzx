package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 操作审计日志实体
 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String role;
    /** 操作类型: CREATE/UPDATE/DELETE/PUBLISH/OFFLINE/SHIP/REFUND_HANDLE/REFUND_APPLY等 */
    private String action;
    /** 对象类型: PRODUCT/CATEGORY/USER/ORDER/TRACE_BATCH */
    private String targetType;
    private Long targetId;
    private String detail;
    private Date createTime;
}
