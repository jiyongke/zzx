package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.BusinessException;
import com.orange.farm.dto.OrderCreateDTO;
import com.orange.farm.dto.OrderItemDTO;
import com.orange.farm.entity.Address;
import com.orange.farm.entity.Cart;
import com.orange.farm.entity.LogisticsLog;
import com.orange.farm.entity.Order;
import com.orange.farm.entity.OrderItem;
import com.orange.farm.entity.Product;
import com.orange.farm.mapper.AddressMapper;
import com.orange.farm.mapper.CartMapper;
import com.orange.farm.mapper.LogisticsLogMapper;
import com.orange.farm.mapper.OrderItemMapper;
import com.orange.farm.mapper.OrderMapper;
import com.orange.farm.mapper.ProductMapper;
import com.orange.farm.util.UserContext;
import com.orange.farm.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private CartMapper cartMapper;

    @Resource
    private LogisticsLogMapper logisticsLogMapper;

    @Resource
    private AuditService auditService;

    /**
     * 创建订单（支持购物车结算与直接购买）
     */
    @Transactional(rollbackFor = Exception.class)
    public Order create(OrderCreateDTO dto) {
        Long userId = UserContext.getUserId();

        List<OrderItemDTO> items = new ArrayList<>();
        if (dto.getFromCart() != null && dto.getFromCart()) {
            List<Cart> carts = cartMapper.selectList(new QueryWrapper<Cart>()
                    .eq("user_id", userId).orderByDesc("create_time"));
            if (carts.isEmpty()) {
                throw new BusinessException("购物车为空，无法结算");
            }
            for (Cart c : carts) {
                OrderItemDTO item = new OrderItemDTO();
                item.setProductId(c.getProductId());
                item.setQuantity(c.getQuantity());
                items.add(item);
            }
        } else {
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                throw new BusinessException("请选择要购买的商品");
            }
            items = dto.getItems();
        }

        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setReceiver(address.getReceiver());
        order.setPhone(address.getPhone());
        order.setAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(dto.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BusinessException("商品不存在或已下架");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }
            BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(amount);

            OrderItem oi = new OrderItem();
            oi.setProductId(product.getId());
            oi.setProductName(product.getName());
            oi.setProductImage(product.getCoverImage());
            oi.setPrice(product.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setAmount(amount);
            orderItems.add(oi);

            // 扣减库存
            product.setStock(product.getStock() - item.getQuantity());
            productMapper.updateById(product);
        }

        order.setTotalAmount(total);
        orderMapper.insert(order);

        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        // 购物车结算时清空购物车
        if (dto.getFromCart() != null && dto.getFromCart()) {
            cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
        }
        return order;
    }

    /**
     * 我的订单分页
     */
    public Page<OrderVO> myOrders(int page, int size, String status) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", UserContext.getUserId());
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("id");
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return toVO(p);
    }

    /**
     * 管理端/农户端订单分页（管理员看全部；农户只看含自己商品的订单）
     */
    public Page<OrderVO> adminOrders(int page, int size, String status, String keyword) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if ("FARMER".equals(UserContext.getRole())) {
            // 仅包含当前农户名下商品的订单（生产主体发货）
            Long farmerId = UserContext.getUserId();
            wrapper.inSql("id", "SELECT DISTINCT oi.order_id FROM order_item oi " +
                    "JOIN product p ON oi.product_id = p.id WHERE p.farmer_id = " + farmerId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("order_no", keyword).or().like("receiver", keyword));
        }
        wrapper.orderByDesc("id");
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return toVO(p);
    }

    public OrderVO detail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 订单本人、管理员、或生产该订单商品的农户可查看
        String role = UserContext.getRole();
        boolean isOwner = order.getUserId().equals(UserContext.getUserId());
        boolean isAdmin = "ADMIN".equals(role);
        if (!isOwner && !isAdmin && !("FARMER".equals(role) && isFarmerOrder(order.getId()))) {
            throw new BusinessException("无权查看该订单");
        }
        return buildVO(order);
    }

    /**
     * 模拟支付
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id) {
        Order order = checkOwnership(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法支付");
        }
        order.setStatus("PAID");
        order.setPayTime(new Date());
        orderMapper.updateById(order);
        // 累加销量
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSales(product.getSales() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
    }

    /**
     * 取消订单（恢复库存）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Order order = checkOwnership(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("仅待支付订单可以取消");
        }
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
    }

    /**
     * 确认收货（需物流已签收）
     */
    public void confirm(Long id) {
        Order order = checkOwnership(id);
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法确认收货");
        }
        if (!"SIGNED".equals(order.getDeliveryStatus())) {
            throw new BusinessException("商品尚未签收，请等待物流更新为「已签收」后再确认收货");
        }
        order.setStatus("COMPLETED");
        order.setCompleteTime(new Date());
        orderMapper.updateById(order);
    }

    /**
     * 发货（农户/管理员）：填写快递公司与运单号，生成物流时间轴首节点
     */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, String courierCompany, String trackingNo) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可以发货");
        }
        checkLogisticsPermission(order.getId());
        if (courierCompany == null || courierCompany.trim().isEmpty()) {
            throw new BusinessException("请填写快递公司");
        }
        if (trackingNo == null || trackingNo.trim().isEmpty()) {
            throw new BusinessException("请填写运单号");
        }
        order.setStatus("SHIPPED");
        order.setDeliveryStatus("SHIPPED");
        order.setShipTime(new Date());
        order.setCourierCompany(courierCompany.trim());
        order.setTrackingNo(trackingNo.trim());
        orderMapper.updateById(order);
        addLogisticsLog(order.getId(), "已发货",
                "快递公司：" + courierCompany.trim() + "，运单号：" + trackingNo.trim());
        auditService.log("SHIP", "ORDER", id, "订单 " + order.getOrderNo() + " 发货（"
                + courierCompany.trim() + " " + trackingNo.trim() + "）");
    }

    /**
     * 更新配送状态：已发货→配送中→已签收（农户/管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDelivery(Long id, String deliveryStatus) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("仅已发货订单可更新配送状态");
        }
        checkLogisticsPermission(id);
        String current = order.getDeliveryStatus();
        if ("DELIVERING".equals(deliveryStatus)) {
            if ("DELIVERING".equals(current) || "SIGNED".equals(current)) {
                throw new BusinessException("配送状态流转不正确（已发货→配送中→已签收）");
            }
        } else if ("SIGNED".equals(deliveryStatus)) {
            if (!"DELIVERING".equals(current)) {
                throw new BusinessException("请先将配送状态更新为「配送中」，再更新为「已签收」");
            }
        } else {
            throw new BusinessException("不支持的配送状态");
        }
        order.setDeliveryStatus(deliveryStatus);
        orderMapper.updateById(order);
        String title = "DELIVERING".equals(deliveryStatus) ? "配送中" : "已签收";
        String desc = "DELIVERING".equals(deliveryStatus)
                ? "商品已由" + nvl(order.getCourierCompany()) + "揽收，正在配送途中（运单号：" + nvl(order.getTrackingNo()) + "）"
                : "商品已签收，感谢购买，欢迎扫码验证溯源信息";
        addLogisticsLog(order.getId(), title, desc);
        auditService.log("DELIVERY", "ORDER", id, "订单 " + order.getOrderNo() + " 配送状态 → " + title);
    }

    /**
     * 消费者申请售后（已支付/已发货订单，原因必填）
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundApply(Long id, String reason) {
        Order order = checkOwnership(id);
        if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("仅已支付或已发货订单可以申请售后");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("请填写售后原因");
        }
        order.setRefundFrom(order.getStatus());
        order.setStatus("REFUNDING");
        order.setRefundReason(reason.trim());
        orderMapper.updateById(order);
        auditService.log("REFUND_APPLY", "ORDER", id, "订单 " + order.getOrderNo() + " 申请售后: " + reason.trim());
    }

    /**
     * 管理员处理售后：同意退款 → REFUNDED（销量回滚）；驳回 → 恢复售后前状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundHandle(Long id, boolean approve) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"REFUNDING".equals(order.getStatus())) {
            throw new BusinessException("该订单不在售后处理中");
        }
        if (approve) {
            order.setStatus("REFUNDED");
            order.setRefundTime(new Date());
            orderMapper.updateById(order);
            // 退款：回滚商品销量
            List<OrderItem> items = orderItemMapper.selectList(
                    new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setSales(Math.max(0, product.getSales() - item.getQuantity()));
                    productMapper.updateById(product);
                }
            }
            auditService.log("REFUND_HANDLE", "ORDER", id,
                    "订单 " + order.getOrderNo() + " 同意退款 ¥" + order.getTotalAmount());
        } else {
            String back = order.getRefundFrom() == null ? "PAID" : order.getRefundFrom();
            order.setStatus(back);
            // 用 UpdateWrapper 显式清空售后字段（MP 默认不更新 null 字段）
            orderMapper.update(order, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Order>()
                    .eq("id", order.getId())
                    .set("refund_reason", null)
                    .set("refund_from", null));
            auditService.log("REFUND_HANDLE", "ORDER", id,
                    "订单 " + order.getOrderNo() + " 驳回售后申请");
        }
    }

    private Order checkOwnership(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    /** 物流履约权限：管理员或生产该订单商品的农户（不设独立物流角色） */
    private void checkLogisticsPermission(Long orderId) {
        String role = UserContext.getRole();
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("FARMER".equals(role) && isFarmerOrder(orderId)) {
            return;
        }
        throw new BusinessException("无权对该订单执行物流履约操作");
    }

    /** 该订单是否包含当前农户（生产主体）的商品 */
    private boolean isFarmerOrder(Long orderId) {
        Long farmerId = UserContext.getUserId();
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", orderId));
        for (OrderItem item : items) {
            Product p = item.getProductId() == null ? null : productMapper.selectById(item.getProductId());
            if (p != null && farmerId.equals(p.getFarmerId())) {
                return true;
            }
        }
        return false;
    }

    /** 追加物流时间轴节点 */
    private void addLogisticsLog(Long orderId, String title, String description) {
        LogisticsLog log = new LogisticsLog();
        log.setOrderId(orderId);
        log.setNodeTime(new Date());
        log.setTitle(title);
        log.setDescription(description);
        logisticsLogMapper.insert(log);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return "OF" + sdf.format(new Date()) + String.format("%04d", new Random().nextInt(10000));
    }

    private Page<OrderVO> toVO(Page<Order> page) {
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::buildVO).collect(Collectors.toList()));
        return voPage;
    }

    private OrderVO buildVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", order.getId())));
        vo.setLogs(logisticsLogMapper.selectList(new QueryWrapper<LogisticsLog>()
                .eq("order_id", order.getId())
                .orderByAsc("id")));
        return vo;
    }
}
