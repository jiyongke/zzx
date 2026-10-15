package com.orange.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orange.farm.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {

    /** 指定日期区间内按天统计销售金额与订单数 */
    @Select("SELECT DATE(pay_time) AS day, SUM(total_amount) AS amount, COUNT(*) AS cnt " +
            "FROM orders WHERE pay_time >= #{start} AND pay_time < #{end} AND status <> 'CANCELLED' " +
            "GROUP BY DATE(pay_time) ORDER BY day")
    List<Map<String, Object>> dailySales(@Param("start") String start, @Param("end") String end);
}
