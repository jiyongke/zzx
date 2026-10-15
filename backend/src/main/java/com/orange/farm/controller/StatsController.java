package com.orange.farm.controller;

import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.service.StatsService;
import com.orange.farm.vo.DashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 数据统计接口（管理端看板）
 */
@RestController
@RequestMapping("/api/admin")
public class StatsController {

    @Resource
    private StatsService statsService;

    /** 数据看板汇总 */
    @GetMapping("/dashboard")
    @RequireRole({"ADMIN"})
    public Result<DashboardVO> dashboard() {
        return Result.success(statsService.dashboard());
    }

    /** 日报：订单数/销售额/商品销量TOP/售后单数 */
    @GetMapping("/report/daily")
    @RequireRole({"ADMIN"})
    public Result<Map<String, Object>> dailyReport(@RequestParam(required = false) String date) {
        return Result.success(statsService.dailyReport(date));
    }

    /** 周报：近7天逐日趋势 + 商品销量TOP + 分类占比 + 客单价 + 售后单数 */
    @GetMapping("/report/weekly")
    @RequireRole({"ADMIN"})
    public Result<Map<String, Object>> weeklyReport(@RequestParam(required = false) String endDate) {
        return Result.success(statsService.weeklyReport(endDate));
    }
}
