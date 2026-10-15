package com.orange.farm.controller;

import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.entity.Category;
import com.orange.farm.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类接口
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /** 前台分类列表（公开） */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    /** 管理端分类列表（含停用） */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN"})
    public Result<List<Category>> adminList() {
        return Result.success(categoryService.adminList());
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody Category category) {
        categoryService.save(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
