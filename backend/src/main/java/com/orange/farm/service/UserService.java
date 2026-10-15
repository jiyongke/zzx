package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.BusinessException;
import com.orange.farm.dto.PasswordDTO;
import com.orange.farm.dto.QualificationDTO;
import com.orange.farm.dto.RegisterDTO;
import com.orange.farm.entity.Address;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.AddressMapper;
import com.orange.farm.mapper.UserMapper;
import com.orange.farm.util.Md5Util;
import com.orange.farm.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 用户服务：个人中心 / 地址管理 / 资质认证 / 管理端用户管理
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private AuditService auditService;

    // ---------------- 个人资料 ----------------

    public User profile() {
        return userMapper.selectById(UserContext.getUserId());
    }

    public void updateProfile(String nickname, String phone) {
        User user = new User();
        user.setId(UserContext.getUserId());
        user.setNickname(nickname);
        user.setPhone(phone);
        userMapper.updateById(user);
    }

    public void changePassword(PasswordDTO dto) {
        User user = userMapper.selectById(UserContext.getUserId());
        if (!user.getPassword().equals(Md5Util.md5(dto.getOldPassword()))) {
            throw new BusinessException("原密码错误");
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(Md5Util.md5(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    // ---------------- 收货地址 ----------------

    public List<Address> addressList() {
        return addressMapper.selectList(new QueryWrapper<Address>()
                .eq("user_id", UserContext.getUserId())
                .orderByDesc("is_default"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addAddress(Address address) {
        address.setId(null);
        address.setUserId(UserContext.getUserId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }
        addressMapper.insert(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long id, Address address) {
        address.setId(id);
        address.setUserId(UserContext.getUserId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }
        addressMapper.updateById(address);
    }

    public void deleteAddress(Long id) {
        addressMapper.delete(new QueryWrapper<Address>()
                .eq("id", id).eq("user_id", UserContext.getUserId()));
    }

    private void clearDefault(Long userId) {
        Address reset = new Address();
        reset.setIsDefault(0);
        addressMapper.update(reset, new QueryWrapper<Address>().eq("user_id", userId));
    }

    // ---------------- 农户资质认证（准入可信） ----------------

    /** 查询当前农户的资质认证信息 */
    public User qualificationInfo() {
        return userMapper.selectById(UserContext.getUserId());
    }

    /**
     * 农户提交实名与资质认证及质量安全材料（进入待审核）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitQualification(QualificationDTO dto) {
        User user = UserContext.get();
        if (!"FARMER".equals(user.getRole())) {
            throw new BusinessException("仅农户需要提交资质认证");
        }
        if (isBlank(dto.getRealName()) || isBlank(dto.getIdCard())
                || isBlank(dto.getQualification()) || isBlank(dto.getQualityMaterial())) {
            throw new BusinessException("请完整填写实名、身份证号、资质证照与质量安全材料");
        }
        User update = new User();
        update.setId(user.getId());
        update.setRealName(dto.getRealName().trim());
        update.setIdCard(dto.getIdCard().trim());
        update.setQualification(dto.getQualification().trim());
        update.setQualityMaterial(dto.getQualityMaterial().trim());
        update.setAuditStatus(1);      // 待审核
        update.setAuditRemark(null);   // 重新提交时清空上次驳回意见
        update.setAuditTime(null);
        userMapper.updateById(update);
        auditService.log("QUALIFICATION_SUBMIT", "USER", user.getId(),
                "农户提交资质认证: " + update.getRealName() + " [" + user.getUsername() + "]");
    }

    /** 管理端资质审核分页（农户账号，可按审核状态筛选） */
    public Page<User> adminQualificationPage(int page, int size, Integer status, String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", "FARMER");
        if (status != null) {
            wrapper.eq("audit_status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("nickname", keyword));
        }
        wrapper.orderByAsc("audit_status").orderByDesc("id");
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 管理员审核农户资质：通过 / 驳回（全程留痕）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewQualification(Long id, boolean approve, String remark) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (!"FARMER".equals(target.getRole())) {
            throw new BusinessException("仅农户账号需要资质审核");
        }
        User update = new User();
        update.setId(id);
        if (approve) {
            update.setAuditStatus(2);
            update.setAuditRemark(null);
        } else {
            if (isBlank(remark)) {
                throw new BusinessException("驳回时请填写审核意见");
            }
            update.setAuditStatus(3);
            update.setAuditRemark(remark.trim());
        }
        update.setAuditTime(new Date());
        userMapper.updateById(update);
        auditService.log(approve ? "QUALIFICATION_APPROVE" : "QUALIFICATION_REJECT", "USER", id,
                (approve ? "资质审核通过: " : "资质审核驳回: ") + target.getUsername()
                        + (approve ? "" : "（" + remark.trim() + "）"));
    }

    /** 校验农户资质是否已通过审核 */
    public static boolean isQualified(User user) {
        return user != null && user.getAuditStatus() != null && user.getAuditStatus() == 2;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---------------- 管理端用户管理 ----------------

    public Page<User> adminUserPage(int page, int size, String keyword, String role) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("username", keyword).or().like("nickname", keyword);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq("role", role);
        }
        wrapper.orderByDesc("id");
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void updateStatus(Long id, Integer status) {
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException("不能禁用自己的账号");
        }
        User old = userMapper.selectById(id);
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
        if (old != null) {
            auditService.log("UPDATE", "USER", id,
                    (status != null && status == 1 ? "启用账号: " : "禁用账号: ") + old.getUsername());
        }
    }

    public void updateRole(Long id, String role) {
        if (!Arrays.asList("ADMIN", "FARMER", "CONSUMER").contains(role)) {
            throw new BusinessException("不支持的账号角色（平台仅设消费者/农户/管理员三类角色）");
        }
        User old = userMapper.selectById(id);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        userMapper.updateById(user);
        if (old != null) {
            auditService.log("UPDATE", "USER", id,
                    "调整账号角色: " + old.getUsername() + " → " + role);
        }
    }

    public void adminCreateUser(RegisterDTO dto, String role) {
        if (!Arrays.asList("ADMIN", "FARMER", "CONSUMER").contains(role)) {
            throw new BusinessException("不支持的账号角色（平台仅设消费者/农户/管理员三类角色）");
        }
        Long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(Md5Util.md5(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isEmpty() ? dto.getUsername() : dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        auditService.log("CREATE", "USER", user.getId(),
                "创建账号: " + dto.getUsername() + "(" + role + ")");
    }
}
