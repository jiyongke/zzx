package com.orange.farm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 物流跟踪记录（订单附属节点，构成订单详情中的物流时间轴）
 */
@Data
@TableName("logistics_log")
public class LogisticsLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** 节点时间 */
    private Date nodeTime;
    /** 节点标题（如：已发货/配送中/已签收） */
    private String title;
    /** 节点说明（快递公司、运单号等） */
    private String description;
}
