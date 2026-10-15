package com.orange.farm.vo;

import com.orange.farm.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private User user;
}
