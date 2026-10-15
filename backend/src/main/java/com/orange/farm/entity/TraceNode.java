package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 溯源环节记录实体
 */
@Data
@TableName("trace_node")
public class TraceNode {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    /** 录入人ID（留痕） */
    private Long recorderId;
    /** 录入人角色 */
    private String recorderRole;
    /** 录入时间（留痕） */
    private Date recorderTime;
}
