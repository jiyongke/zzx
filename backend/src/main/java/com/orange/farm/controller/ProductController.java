package com.orange.farm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.entity.Product;
import com.orange.farm.service.ProductService;
import com.orange.farm.vo.ProductVO;
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
 * 商品接口
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;

    /** 前台商品分页列表（公开） */
    @GetMapping("/list")
    public Result<Page<ProductVO>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "12") int size,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String sort) {
        return Result.success(productService.list(page, size, categoryId, keyword, sort));
    }

    /** 首页热销推荐（公开） */
    @GetMapping("/hot")
    public Result<List<ProductVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.hotList(limit));
    }

    /** 商品详情（公开） */
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    /** 管理端商品分页 */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN"})
    public Result<Page<ProductVO>> adminList(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(productService.adminList(page, size, keyword));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody Product product) {
        productService.save(product);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.update(product);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireRole({"ADMIN"})
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    // ---------------- 农户端：我的商品 ----------------

    /** 农户商品分页（仅本人） */
    @GetMapping("/farmer/list")
    @RequireRole({"FARMER"})
    public Result<Page<ProductVO>> farmerList(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(productService.farmerList(page, size, keyword));
    }

    /** 农户新增商品（默认待审核） */
    @PostMapping("/farmer")
    @RequireRole({"FARMER"})
    public Result<Void> farmerSave(@RequestBody Product product) {
        productService.farmerSave(product);
        return Result.success();
    }

    /** 农户编辑自己的商品（改价/库存/图文等） */
    @PutMapping("/farmer/{id}")
    @RequireRole({"FARMER"})
    public Result<Void> farmerUpdate(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.farmerUpdate(product);
        return Result.success();
    }

    /** 农户申请上架（重新提交审核） */
    @PutMapping("/farmer/{id}/apply")
    @RequireRole({"FARMER"})
    public Result<Void> farmerApplyOnShelf(@PathVariable Long id) {
        productService.farmerApplyOnShelf(id);
        return Result.success();
    }
}
