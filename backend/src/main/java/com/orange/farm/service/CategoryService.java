package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.orange.farm.common.BusinessException;
import com.orange.farm.entity.Category;
import com.orange.farm.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类服务
 */
@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private AuditService auditService;

    public List<Category> list() {
        return categoryMapper.selectList(new QueryWrapper<Category>()
                .eq("status", 1)
                .orderByAsc("sort"));
    }

    public List<Category> adminList() {
        return categoryMapper.selectList(new QueryWrapper<Category>().orderByAsc("sort"));
    }

    public void save(Category category) {
        category.setId(null);
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
        auditService.log("CREATE", "CATEGORY", category.getId(), "新增分类: " + category.getName());
    }

    public void update(Category category) {
        if (category.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        categoryMapper.updateById(category);
        auditService.log("UPDATE", "CATEGORY", category.getId(), "修改分类: " + category.getName());
    }

    public void delete(Long id) {
        Category old = categoryMapper.selectById(id);
        if (old == null) {
            throw new BusinessException("分类不存在");
        }
        categoryMapper.deleteById(id);
        auditService.log("DELETE", "CATEGORY", id, "删除分类: " + old.getName());
    }
}
