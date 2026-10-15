package com.orange.farm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 20, message = "账号长度需在3-20位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;

    private String nickname;

    private String phone;

    /** 注册角色: CONSUMER默认 / FARMER（农户需资质审核）；ADMIN不可通过公开注册 */
    private String role;
}
