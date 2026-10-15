package com.orange.farm.dto;

import lombok.Data;

import java.util.Date;

/**
 * 溯源环节录入/编辑 DTO
 */
@Data
public class TraceNodeDTO {

    private Long batchId;

    /** 环节类型: PLANT/FERTILIZE/PEST/HARVEST/PROCESS/QUALITY/STORAGE/LOGISTICS */
    private String nodeType;

    private String title;

    private String description;

    /** 图片URL，逗号分隔 */
    private String images;

    private String operator;

    private String location;

    private Date recordTime;

    private Integer sort;
}
