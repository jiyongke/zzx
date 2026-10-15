package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** 密码不回传给前端 */
    @JsonIgnore
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    /** 角色: ADMIN/FARMER/CONSUMER */
    private String role;
    /** 状态: 1正常 0禁用 */
    private Integer status;
    private Date createTime;

    // ---- 农户实名与资质认证（准入可信） ----
    /** 实名姓名 */
    private String realName;
    /** 身份证号 */
    private String idCard;
    /** 资质证照/营业执照（URL） */
    private String qualification;
    /** 质量安全材料（检测报告、产地证明等，URL逗号分隔） */
    private String qualityMaterial;
    /** 资质审核状态: 0未提交 1待审核 2已通过 3已驳回 */
    private Integer auditStatus;
    /** 审核意见（驳回原因） */
    private String auditRemark;
    /** 审核时间 */
    private Date auditTime;
}
