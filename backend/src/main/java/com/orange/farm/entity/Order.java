package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    /** 状态: PENDING待支付/PAID已支付/SHIPPED已发货/COMPLETED已完成/CANCELLED已取消/REFUNDING售后中/REFUNDED已退款 */
    private String status;
    private String receiver;
    private String phone;
    private String address;
    private String remark;
    private String refundReason;
    /** 售后来源状态（售后中→驳回时恢复）: PAID/SHIPPED */
    private String refundFrom;
    private Date payTime;
    private Date shipTime;
    private Date completeTime;
    private Date refundTime;
    /** 快递公司（发货时填写） */
    private String courierCompany;
    /** 运单号（发货时填写） */
    private String trackingNo;
    /** 配送状态: SHIPPED已发货/DELIVERING配送中/SIGNED已签收（物流履约随订单流转，不设独立物流角色） */
    private String deliveryStatus;
    private Date createTime;
}
