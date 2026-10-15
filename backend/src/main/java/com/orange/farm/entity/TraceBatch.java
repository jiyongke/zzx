package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 溯源批次实体（核心）
 */
@Data
@TableName("trace_batch")
public class TraceBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 溯源批次号（二维码内容） */
    private String batchCode;
    private String productName;
    private Long categoryId;
    private String origin;
    private Long farmerId;
    private String farmerName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date plantDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date harvestDate;
    private String description;
    private String coverImage;
    /** 状态: 0待完善 1可溯源 2已下架 3待审核 */
    private Integer status;
    /** 溯源链摘要哈希（SHA-256），发布时计算入库，查询时重新计算比对（存证可信） */
    private String hashValue;
    /** 累计扫码查询次数 */
    private Integer scanCount;
    /** 最近一次扫码查询时间 */
    private Date lastScanTime;
    private Date createTime;
}
