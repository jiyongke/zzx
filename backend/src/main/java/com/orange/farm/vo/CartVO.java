package com.orange.farm.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车展示对象（含商品信息）
 */
@Data
public class CartVO {

    private Long id;
    private Long productId;
    private String name;
    private String coverImage;
    private BigDecimal price;
    private Integer quantity;
    private Integer stock;
    private Integer sales;
    /** 是否可溯源 */
    private Boolean traceable;
}
