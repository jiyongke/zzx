package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.BusinessException;
import com.orange.farm.entity.Product;
import com.orange.farm.entity.TraceBatch;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.CategoryMapper;
import com.orange.farm.mapper.ProductMapper;
import com.orange.farm.mapper.TraceBatchMapper;
import com.orange.farm.util.UserContext;
import com.orange.farm.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务
 */
@Service
public class ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private TraceBatchMapper traceBatchMapper;

    @Resource
    private AuditService auditService;

    /**
     * 前台商品分页列表（仅上架）
     */
    public Page<ProductVO> list(int page, int size, Long categoryId, String keyword, String sort) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("name", keyword);
        }
        if ("sales".equals(sort)) {
            wrapper.orderByDesc("sales");
        } else if ("price_asc".equals(sort)) {
            wrapper.orderByAsc("price");
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc("price");
        } else {
            wrapper.orderByDesc("id");
        }
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), wrapper);
        return toVO(p);
    }

    /**
     * 首页热销推荐
     */
    public List<ProductVO> hotList(int limit) {
        Page<Product> p = productMapper.selectPage(
                new Page<>(1, limit),
                new QueryWrapper<Product>().eq("status", 1).orderByDesc("sales"));
        return toVO(p).getRecords();
    }

    public ProductVO detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return toVO(product);
    }

    /**
     * 管理端商品分页（含下架）
     */
    public Page<ProductVO> adminList(int page, int size, String keyword) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("id");
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), wrapper);
        return toVO(p);
    }

    public void save(Product product) {
        product.setId(null);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        productMapper.insert(product);
        auditService.log("CREATE", "PRODUCT", product.getId(), "新增商品: " + product.getName());
    }

    public void update(Product product) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        productMapper.updateById(product);
        auditService.log("UPDATE", "PRODUCT", product.getId(), "修改商品: " + product.getName());
    }

    public void updateStatus(Long id, Integer status) {
        Product old = productMapper.selectById(id);
        if (old == null) {
            throw new BusinessException("商品不存在");
        }
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        productMapper.updateById(product);
        auditService.log(status != null && status == 1 ? "ON_SHELF" : "OFF_SHELF",
                "PRODUCT", id, (status != null && status == 1 ? "商品上架: " : "商品下架: ") + old.getName());
    }

    public void delete(Long id) {
        Product old = productMapper.selectById(id);
        if (old == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(id);
        auditService.log("DELETE", "PRODUCT", id, "删除商品: " + old.getName());
    }

    // ---------------- 农户端：我的商品 ----------------

    /**
     * 农户商品分页（仅本人，含全部状态）
     */
    public Page<ProductVO> farmerList(int page, int size, String keyword) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("farmer_id", UserContext.getUserId());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("id");
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), wrapper);
        return toVO(p);
    }

    /**
     * 农户新增商品（默认待审核；须先通过资质认证）
     */
    public void farmerSave(Product product) {
        User user = UserContext.get();
        if (!UserService.isQualified(user)) {
            throw new BusinessException("您尚未通过实名与资质认证，请先在个人中心提交资质认证并经管理员审核通过后再上架商品");
        }
        product.setId(null);
        product.setFarmerId(user.getId());
        product.setFarmerName(user.getNickname());
        product.setSales(0);
        product.setStatus(3);
        checkBatchOwned(product.getBatchId(), user.getId());
        productMapper.insert(product);
        auditService.log("CREATE", "PRODUCT", product.getId(),
                "农户新增商品(待审核): " + product.getName() + " [" + user.getNickname() + "]");
    }

    /**
     * 农户编辑自己的商品（改价/库存/图文/名称/分类/批次；状态与销量不可改）
     */
    public void farmerUpdate(Product product) {
        Long userId = UserContext.getUserId();
        Product old = checkFarmerProduct(product.getId(), userId);
        Product update = new Product();
        update.setId(product.getId());
        update.setName(product.getName());
        update.setCategoryId(product.getCategoryId());
        update.setBatchId(product.getBatchId());
        update.setPrice(product.getPrice());
        update.setStock(product.getStock());
        update.setCoverImage(product.getCoverImage());
        update.setImages(product.getImages());
        update.setDescription(product.getDescription());
        checkBatchOwned(update.getBatchId(), userId);
        productMapper.updateById(update);
        auditService.log("UPDATE", "PRODUCT", product.getId(),
                "农户修改商品: " + old.getName() + " → " + product.getName());
    }

    /**
     * 农户申请上架（下架/审核未通过 → 重新提交审核；须先通过资质认证）
     */
    public void farmerApplyOnShelf(Long id) {
        Long userId = UserContext.getUserId();
        if (!UserService.isQualified(UserContext.get())) {
            throw new BusinessException("您尚未通过实名与资质认证，请先在个人中心提交资质认证并经管理员审核通过后再上架商品");
        }
        Product old = checkFarmerProduct(id, userId);
        if (old.getStatus() != null && old.getStatus() == 1) {
            throw new BusinessException("商品已是上架状态，无需重复申请");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(3);
        productMapper.updateById(update);
        auditService.log("UPDATE", "PRODUCT", id,
                "农户申请上架: " + old.getName() + "（状态 → 待审核）");
    }

    /** 校验商品归属当前农户，返回原商品 */
    private Product checkFarmerProduct(Long id, Long userId) {
        if (id == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Product old = productMapper.selectById(id);
        if (old == null) {
            throw new BusinessException("商品不存在");
        }
        if (old.getFarmerId() == null || !old.getFarmerId().equals(userId)) {
            throw new BusinessException(403, "无权操作该商品");
        }
        return old;
    }

    /** 商品关联的溯源批次必须属于当前农户 */
    private void checkBatchOwned(Long batchId, Long userId) {
        if (batchId == null) {
            return;
        }
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("关联的溯源批次不存在");
        }
        if (!userId.equals(batch.getFarmerId())) {
            throw new BusinessException("只能关联自己名下的溯源批次");
        }
    }

    // ---------------- 组装 VO ----------------

    private Page<ProductVO> toVO(Page<Product> page) {
        Page<ProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        if (product.getCategoryId() != null) {
            com.orange.farm.entity.Category c = categoryMapper.selectById(product.getCategoryId());
            if (c != null) {
                vo.setCategoryName(c.getName());
            }
        }
        if (product.getBatchId() != null) {
            TraceBatch batch = traceBatchMapper.selectById(product.getBatchId());
            if (batch != null) {
                vo.setBatchCode(batch.getBatchCode());
                vo.setTraceable(batch.getStatus() != null && batch.getStatus() == 1);
            } else {
                vo.setTraceable(false);
            }
        }
        return vo;
    }
}
