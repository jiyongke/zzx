package com.orange.farm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板统计
 */
@Data
public class DashboardVO {

    /** 用户总数 */
    private Long userCount;
    /** 在售商品数 */
    private Long productCount;
    /** 溯源批次总数 */
    private Long batchCount;
    /** 订单总数 */
    private Long orderCount;
    /** 累计销售额（已支付及以上） */
    private BigDecimal totalSales;
    /** 今日订单数 */
    private Long todayOrders;
    /** 待发货订单数 */
    private Long pendingShip;

    /** 近7日销售趋势: [{day, amount, cnt}] */
    private List<Map<String, Object>> dailySales;

    /** 分类销售占比: [{name, value}] */
    private List<Map<String, Object>> categorySales;

    /** 溯源批次状态分布: [{name, value}] */
    private List<Map<String, Object>> batchStatus;

    /** 最新订单列表 */
    private List<OrderVO> recentOrders;
}
