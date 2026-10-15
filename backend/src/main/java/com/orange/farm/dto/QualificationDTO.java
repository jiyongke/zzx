package com.orange.farm.dto;

import lombok.Data;

/**
 * 农户资质认证提交 DTO（准入可信）
 */
@Data
public class QualificationDTO {

    /** 实名姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 资质证照/营业执照（URL） */
    private String qualification;

    /** 质量安全材料（检测报告、产地证明等，URL逗号分隔） */
    private String qualityMaterial;
}
