package com.orange.farm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.dto.OrderCreateDTO;
import com.orange.farm.entity.Order;
import com.orange.farm.service.OrderService;
import com.orange.farm.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /** 创建订单 */
    @PostMapping("/create")
    public Result<Order> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(dto));
    }

    /** 我的订单分页 */
    @GetMapping("/list")
    public Result<Page<OrderVO>> myOrders(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String status) {
        return Result.success(orderService.myOrders(page, size, status));
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 模拟支付 */
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    /** 取消订单 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    /** 确认收货 */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    /** 消费者申请售后（已支付/已发货订单） */
    @PostMapping("/{id}/refund-apply")
    public Result<Void> refundApply(@PathVariable Long id,
                                    @RequestParam String reason) {
        orderService.refundApply(id, reason);
        return Result.success();
    }

    /** 管理员处理售后：approve=true 同意退款 / false 驳回 */
    @PutMapping("/admin/{id}/refund-handle")
    @RequireRole({"ADMIN"})
    public Result<Void> refundHandle(@PathVariable Long id,
                                     @RequestParam boolean approve) {
        orderService.refundHandle(id, approve);
        return Result.success();
    }

    /** 发货（农户/管理员）：填写快递公司与运单号 */
    @PutMapping("/{id}/ship")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> ship(@PathVariable Long id,
                             @RequestParam String courierCompany,
                             @RequestParam String trackingNo) {
        orderService.ship(id, courierCompany, trackingNo);
        return Result.success();
    }

    /** 更新配送状态（已发货→配送中→已签收） */
    @PutMapping("/{id}/delivery")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> updateDelivery(@PathVariable Long id,
                                       @RequestParam String deliveryStatus) {
        orderService.updateDelivery(id, deliveryStatus);
        return Result.success();
    }

    /** 全量订单分页（管理员全部；农户仅自己商品的订单） */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Page<OrderVO>> adminOrders(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(orderService.adminOrders(page, size, status, keyword));
    }
}
