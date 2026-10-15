package com.orange.farm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品展示对象（含分类名称）
 */
@Data
public class ProductVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long batchId;
    private String batchCode;
    private Long farmerId;
    private String farmerName;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String coverImage;
    private String images;
    private String description;
    private Integer status;
    private Date createTime;
    /** 是否可溯源 */
    private Boolean traceable;
}
