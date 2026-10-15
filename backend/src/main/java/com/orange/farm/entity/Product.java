package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    /** 关联溯源批次ID */
    private Long batchId;
    /** 生产主体(农户)ID，平台自营为空 */
    private Long farmerId;
    /** 生产主体(农户)名称 */
    private String farmerName;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String coverImage;
    /** 轮播图，逗号分隔 */
    private String images;
    private String description;
    /** 状态: 1上架 0下架 3待审核 */
    private Integer status;
    private Date createTime;
}
