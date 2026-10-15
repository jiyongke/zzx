package com.orange.farm.vo;

import com.orange.farm.entity.LogisticsLog;
import com.orange.farm.entity.Order;
import com.orange.farm.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单展示对象（含明细与物流轨迹）
 */
@Data
public class OrderVO {

    private Order order;
    private List<OrderItem> items;
    /** 物流跟踪时间轴 */
    private List<LogisticsLog> logs;
}
