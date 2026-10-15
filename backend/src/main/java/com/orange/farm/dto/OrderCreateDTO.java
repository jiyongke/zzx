package com.orange.farm.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OrderCreateDTO {

    /** 直接购买时传商品列表；从购物车结算时不传（后端读取购物车） */
    @Valid
    private List<OrderItemDTO> items;

    /** 是否从购物车结算 */
    private Boolean fromCart;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    private String remark;
}
