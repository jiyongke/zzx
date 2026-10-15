package com.orange.farm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.dto.TraceNodeDTO;
import com.orange.farm.entity.TraceBatch;
import com.orange.farm.service.TraceService;
import com.orange.farm.vo.TraceBatchVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 溯源接口（核心模块）
 */
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    @Resource
    private TraceService traceService;

    // ---------------- 公开查询 ----------------

    /** 可溯源批次列表（公开） */
    @GetMapping("/list")
    public Result<List<TraceBatch>> list() {
        return Result.success(traceService.publicList());
    }

    /** 批次详情（公开） */
    @GetMapping("/batch/{id}")
    public Result<TraceBatchVO> batchDetail(@PathVariable Long id) {
        return Result.success(traceService.detail(id));
    }

    /** 二维码/批次号溯源查询（公开，核心） */
    @GetMapping("/query")
    public Result<TraceBatchVO> query(@RequestParam String code) {
        return Result.success(traceService.queryByCode(code));
    }

    // ---------------- 农户/管理员管理 ----------------

    /** 批次管理分页 */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Page<TraceBatch>> batchPage(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(traceService.batchPage(page, size, keyword));
    }

    /** 批次详情（含未发布） */
    @GetMapping("/admin/batch/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<TraceBatchVO> adminBatchDetail(@PathVariable Long id) {
        return Result.success(traceService.batchDetail(id));
    }

    /** 创建溯源批次 */
    @PostMapping("/batch")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> createBatch(@RequestBody TraceBatch batch) {
        traceService.createBatch(batch);
        return Result.success();
    }

    /** 更新溯源批次 */
    @PutMapping("/batch/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody TraceBatch batch) {
        batch.setId(id);
        traceService.updateBatch(batch);
        return Result.success();
    }

    /**
     * 提交发布：农户 0→3（待管理员审核）；管理员 0→1（直接发布并哈希存证）
     */
    @PutMapping("/batch/{id}/publish")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> publish(@PathVariable Long id) {
        traceService.publish(id);
        return Result.success();
    }

    /**
     * 管理员审核溯源链发布申请：approve=true 通过（3→1，哈希存证）/ false 驳回（3→0）
     */
    @PutMapping("/batch/{id}/review")
    @RequireRole({"ADMIN"})
    public Result<Void> review(@PathVariable Long id,
                               @RequestParam boolean approve,
                               @RequestParam(required = false) String remark) {
        traceService.review(id, approve, remark);
        return Result.success();
    }

    /** 下架溯源批次（1→2） */
    @PutMapping("/batch/{id}/offline")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> offline(@PathVariable Long id) {
        traceService.offline(id);
        return Result.success();
    }

    /** 删除批次（连带环节） */
    @DeleteMapping("/batch/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> deleteBatch(@PathVariable Long id) {
        traceService.deleteBatch(id);
        return Result.success();
    }

    /** 录入溯源环节 */
    @PostMapping("/node")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> addNode(@RequestBody TraceNodeDTO dto) {
        traceService.addNode(dto);
        return Result.success();
    }

    /** 删除溯源环节 */
    @DeleteMapping("/node/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> deleteNode(@PathVariable Long id) {
        traceService.deleteNode(id);
        return Result.success();
    }
}
