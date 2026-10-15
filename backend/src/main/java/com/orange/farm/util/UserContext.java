package com.orange.farm.util;

import com.orange.farm.common.BusinessException;
import com.orange.farm.entity.User;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        User user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user;
    }

    public static Long getUserId() {
        return get().getId();
    }

    public static String getRole() {
        return get().getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
