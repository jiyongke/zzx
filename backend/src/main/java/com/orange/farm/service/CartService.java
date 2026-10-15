package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.orange.farm.common.BusinessException;
import com.orange.farm.dto.CartAddDTO;
import com.orange.farm.entity.Cart;
import com.orange.farm.entity.Product;
import com.orange.farm.entity.TraceBatch;
import com.orange.farm.mapper.CartMapper;
import com.orange.farm.mapper.ProductMapper;
import com.orange.farm.mapper.TraceBatchMapper;
import com.orange.farm.util.UserContext;
import com.orange.farm.vo.CartVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
public class CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private TraceBatchMapper traceBatchMapper;

    public List<CartVO> list() {
        Long userId = UserContext.getUserId();
        List<Cart> carts = cartMapper.selectList(new QueryWrapper<Cart>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
        return carts.stream().map(c -> {
            Product product = productMapper.selectById(c.getProductId());
            CartVO vo = new CartVO();
            vo.setId(c.getId());
            if (product == null) {
                vo.setProductId(c.getProductId());
                vo.setQuantity(c.getQuantity());
                return vo;
            }
            vo.setProductId(product.getId());
            vo.setName(product.getName());
            vo.setCoverImage(product.getCoverImage());
            vo.setPrice(product.getPrice());
            vo.setQuantity(c.getQuantity());
            vo.setStock(product.getStock());
            vo.setSales(product.getSales());
            if (product.getBatchId() != null) {
                TraceBatch batch = traceBatchMapper.selectById(product.getBatchId());
                vo.setTraceable(batch != null && batch.getStatus() != null && batch.getStatus() == 1);
            } else {
                vo.setTraceable(false);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    public void add(CartAddDTO dto) {
        Long userId = UserContext.getUserId();
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        Cart exist = cartMapper.selectOne(new QueryWrapper<Cart>()
                .eq("user_id", userId).eq("product_id", dto.getProductId()));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(dto.getProductId());
            cart.setQuantity(dto.getQuantity());
            cartMapper.insert(cart);
        }
    }

    public void updateQuantity(Long id, Integer quantity) {
        Cart cart = cartMapper.selectOne(new QueryWrapper<Cart>()
                .eq("id", id).eq("user_id", UserContext.getUserId()));
        if (cart == null) {
            throw new BusinessException("购物车项不存在");
        }
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    public void delete(Long id) {
        cartMapper.delete(new QueryWrapper<Cart>()
                .eq("id", id).eq("user_id", UserContext.getUserId()));
    }

    public void clear() {
        cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", UserContext.getUserId()));
    }
}
