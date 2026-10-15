package com.orange.farm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.Result;
import com.orange.farm.common.annotation.RequireRole;
import com.orange.farm.dto.PasswordDTO;
import com.orange.farm.dto.QualificationDTO;
import com.orange.farm.dto.RegisterDTO;
import com.orange.farm.entity.Address;
import com.orange.farm.entity.User;
import com.orange.farm.service.UserService;
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
import javax.validation.Valid;
import java.util.List;

/**
 * 用户接口：个人中心 / 地址管理 / 管理端用户管理
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Resource
    private UserService userService;

    // ---------------- 个人中心 ----------------

    @PutMapping("/user/profile")
    public Result<Void> updateProfile(@RequestParam(required = false) String nickname,
                                      @RequestParam(required = false) String phone) {
        userService.updateProfile(nickname, phone);
        return Result.success();
    }

    @PutMapping("/user/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }

    // ---------------- 收货地址 ----------------

    @GetMapping("/user/address/list")
    public Result<List<Address>> addressList() {
        return Result.success(userService.addressList());
    }

    @PostMapping("/user/address")
    public Result<Void> addAddress(@RequestBody Address address) {
        userService.addAddress(address);
        return Result.success();
    }

    @PutMapping("/user/address/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Address address) {
        userService.updateAddress(id, address);
        return Result.success();
    }

    @DeleteMapping("/user/address/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return Result.success();
    }

    // ---------------- 农户资质认证（准入可信） ----------------

    /** 当前农户资质认证信息 */
    @GetMapping("/user/qualification")
    public Result<User> qualificationInfo() {
        return Result.success(userService.qualificationInfo());
    }

    /** 农户提交实名与资质认证及质量安全材料 */
    @PostMapping("/user/qualification")
    public Result<Void> submitQualification(@RequestBody QualificationDTO dto) {
        userService.submitQualification(dto);
        return Result.success();
    }

    /** 管理端资质审核分页 */
    @GetMapping("/admin/qualification/list")
    @RequireRole({"ADMIN"})
    public Result<Page<User>> adminQualificationPage(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(userService.adminQualificationPage(page, size, status, keyword));
    }

    /** 管理员审核农户资质：approve=true 通过 / false 驳回（remark 必填） */
    @PutMapping("/admin/qualification/{id}/review")
    @RequireRole({"ADMIN"})
    public Result<Void> reviewQualification(@PathVariable Long id,
                                            @RequestParam boolean approve,
                                            @RequestParam(required = false) String remark) {
        userService.reviewQualification(id, approve, remark);
        return Result.success();
    }

    // ---------------- 管理端用户管理 ----------------

    @GetMapping("/admin/user/list")
    @RequireRole({"ADMIN"})
    public Result<Page<User>> adminUserPage(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String role) {
        return Result.success(userService.adminUserPage(page, size, keyword, role));
    }

    @PutMapping("/admin/user/{id}/status")
    @RequireRole({"ADMIN"})
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/admin/user/{id}/role")
    @RequireRole({"ADMIN"})
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateRole(id, role);
        return Result.success();
    }

    @PostMapping("/admin/user")
    @RequireRole({"ADMIN"})
    public Result<Void> adminCreateUser(@Valid @RequestBody RegisterDTO dto,
                                        @RequestParam(defaultValue = "CONSUMER") String role) {
        userService.adminCreateUser(dto, role);
        return Result.success();
    }
}
