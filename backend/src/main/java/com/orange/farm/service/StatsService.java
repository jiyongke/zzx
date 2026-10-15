package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.orange.farm.entity.Order;
import com.orange.farm.entity.OrderItem;
import com.orange.farm.entity.Product;
import com.orange.farm.entity.TraceBatch;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.CategoryMapper;
import com.orange.farm.mapper.OrderItemMapper;
import com.orange.farm.mapper.OrderMapper;
import com.orange.farm.mapper.ProductMapper;
import com.orange.farm.mapper.TraceBatchMapper;
import com.orange.farm.mapper.UserMapper;
import com.orange.farm.vo.DashboardVO;
import com.orange.farm.vo.OrderVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计服务
 */
@Service
public class StatsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private TraceBatchMapper traceBatchMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private CategoryMapper categoryMapper;

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();

        // 基础计数
        vo.setUserCount(userMapper.selectCount(null));
        vo.setProductCount(productMapper.selectCount(
                new QueryWrapper<Product>().eq("status", 1)));
        vo.setBatchCount(traceBatchMapper.selectCount(null));
        vo.setOrderCount(orderMapper.selectCount(null));

        // 累计销售额（已支付及以上）
        List<Order> paidOrders = orderMapper.selectList(new QueryWrapper<Order>()
                .in("status", "PAID", "SHIPPED", "COMPLETED"));
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : paidOrders) {
            total = total.add(o.getTotalAmount());
        }
        vo.setTotalSales(total);

        // 今日订单数
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        vo.setTodayOrders(orderMapper.selectCount(new QueryWrapper<Order>()
                .like("create_time", today)));
        vo.setPendingShip(orderMapper.selectCount(new QueryWrapper<Order>()
                .eq("status", "PAID")));

        // 近7日销售趋势
        vo.setDailySales(dailySales(7));

        // 分类销售占比
        vo.setCategorySales(productMapper.categorySales());

        // 溯源批次状态分布
        vo.setBatchStatus(traceBatchMapper.countByStatus());

        // 最新5条订单
        List<Order> recent = orderMapper.selectList(new QueryWrapper<Order>()
                .orderByDesc("id").last("LIMIT 5"));
        List<OrderVO> recentOrders = new ArrayList<>();
        for (Order o : recent) {
            OrderVO ov = new OrderVO();
            ov.setOrder(o);
            ov.setItems(orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                    .eq("order_id", o.getId())));
            recentOrders.add(ov);
        }
        vo.setRecentOrders(recentOrders);
        return vo;
    }

    /**
     * 近 N 天销售趋势（含补零）
     */
    private List<Map<String, Object>> dailySales(int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1));
        String start = sdf.format(cal.getTime()) + " 00:00:00";
        String end = sdf.format(new Date()) + " 23:59:59";

        List<Map<String, Object>> rows = orderMapper.dailySales(start, end);

        // 组装日期序列并补零
        Map<String, Map<String, Object>> byDay = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String day = sdf.format(cal.getTime());
            Map<String, Object> item = byDay.get(day);
            if (item == null) {
                item = new LinkedHashMap<>();
                item.put("day", day);
                item.put("amount", BigDecimal.ZERO);
                item.put("cnt", 0);
            }
            result.add(item);
        }
        // 反转为正序
        java.util.Collections.reverse(result);
        return result;
    }

    // ---------------- 日报 / 周报 ----------------

    /** 已支付口径（计入销售额的订单状态） */
    private static final String[] SALE_STATUS = {"PAID", "SHIPPED", "COMPLETED"};

    /**
     * 日报：指定日期的订单数、销售额、商品销量 TOP5、售后单数
     */
    public Map<String, Object> dailyReport(String date) {
        if (date == null || date.isEmpty()) {
            date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        }
        List<Order> dayOrders = orderMapper.selectList(new QueryWrapper<Order>()
                .like("create_time", date));
        return buildReport(dayOrders, date);
    }

    /**
     * 周报：近 7 天逐日订单数/销售额 + 商品销量 TOP5 + 分类占比 + 客单价 + 售后单数
     */
    public Map<String, Object> weeklyReport(String endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        if (endDate != null && !endDate.isEmpty()) {
            try {
                cal.setTime(sdf.parse(endDate));
            } catch (Exception ignored) {
            }
        }
        cal.add(Calendar.DAY_OF_MONTH, -6);
        String start = sdf.format(cal.getTime()) + " 00:00:00";
        cal.add(Calendar.DAY_OF_MONTH, 6);
        String end = sdf.format(cal.getTime()) + " 23:59:59";

        List<Order> weekOrders = orderMapper.selectList(new QueryWrapper<Order>()
                .ge("create_time", start).le("create_time", end));

        Map<String, Object> report = buildReport(weekOrders, "近7天");

        // 近 7 日逐日趋势
        report.put("dailyTrend", dailySales(7));
        return report;
    }

    /**
     * 报表公共聚合：订单数/销售额/商品销量TOP5/分类占比/客单价/售后单数
     */
    private Map<String, Object> buildReport(List<Order> orders, String label) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", label);

        BigDecimal sales = BigDecimal.ZERO;
        int saleCnt = 0;
        int refundCnt = 0;
        for (Order o : orders) {
            String st = o.getStatus();
            boolean onSale = false;
            for (String s : SALE_STATUS) {
                if (s.equals(st)) {
                    onSale = true;
                    break;
                }
            }
            if (onSale) {
                sales = sales.add(o.getTotalAmount());
                saleCnt++;
            }
            if ("REFUNDING".equals(st) || "REFUNDED".equals(st)) {
                refundCnt++;
            }
        }
        report.put("orderCount", orders.size());
        report.put("saleCount", saleCnt);
        report.put("sales", sales);
        report.put("refundCount", refundCnt);
        report.put("avgOrderAmount", saleCnt == 0 ? BigDecimal.ZERO
                : sales.divide(BigDecimal.valueOf(saleCnt), 2, java.math.RoundingMode.HALF_UP));

        // 商品销量 TOP5（按该批订单明细聚合）
        Map<String, Object[]> productAgg = new LinkedHashMap<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                    .eq("order_id", o.getId()));
            for (OrderItem it : items) {
                Object[] agg = productAgg.computeIfAbsent(it.getProductName(),
                        k -> new Object[]{0, BigDecimal.ZERO});
                agg[0] = (Integer) agg[0] + it.getQuantity();
                agg[1] = ((BigDecimal) agg[1]).add(it.getAmount());
            }
        }
        List<Map<String, Object>> topProducts = new ArrayList<>();
        productAgg.forEach((name, agg) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("quantity", agg[0]);
            m.put("amount", agg[1]);
            topProducts.add(m);
        });
        topProducts.sort((a, b) -> (Integer) b.get("quantity") - (Integer) a.get("quantity"));
        report.put("topProducts", topProducts.size() > 5 ? topProducts.subList(0, 5) : topProducts);

        // 分类销售额占比（商品 → 分类）
        Map<String, BigDecimal> catAgg = new LinkedHashMap<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                    .eq("order_id", o.getId()));
            for (OrderItem it : items) {
                Product p = it.getProductId() == null ? null : productMapper.selectById(it.getProductId());
                String catName = "其他";
                if (p != null && p.getCategoryId() != null) {
                    com.orange.farm.entity.Category c = categoryMapper.selectById(p.getCategoryId());
                    if (c != null) {
                        catName = c.getName();
                    }
                }
                catAgg.merge(catName, it.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> catSales = new ArrayList<>();
        catAgg.forEach((name, amount) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("amount", amount);
            catSales.add(m);
        });
        report.put("categorySales", catSales);
        return report;
    }
}
