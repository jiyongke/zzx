package com.orange.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orange.farm.entity.Product;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ProductMapper extends BaseMapper<Product> {

    /** 分类销售金额统计（用于管理端看板） */
    @Select("SELECT c.name AS name, COALESCE(SUM(oi.amount), 0) AS value " +
            "FROM order_item oi " +
            "JOIN orders o ON oi.order_id = o.id AND o.status <> 'CANCELLED' " +
            "JOIN product p ON oi.product_id = p.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "GROUP BY c.id, c.name ORDER BY value DESC")
    List<Map<String, Object>> categorySales();
}
