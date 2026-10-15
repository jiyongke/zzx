package com.orange.farm.controller;

import com.orange.farm.common.Result;
import com.orange.farm.dto.CartAddDTO;
import com.orange.farm.dto.CartUpdateDTO;
import com.orange.farm.service.CartService;
import com.orange.farm.vo.CartVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @GetMapping("/list")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list());
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(id, dto.getQuantity());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.success();
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }
}
