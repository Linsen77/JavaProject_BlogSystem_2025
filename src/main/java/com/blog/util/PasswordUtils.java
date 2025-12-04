package com.blog.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//密码加密和校验工具类
public class PasswordUtils {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 加密密码（不可逆）
    public static String encryptPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 校验密码（原密码 vs 加密后的密码）
    public static boolean checkPassword(String rawPassword, String encryptedPassword) {
        return encoder.matches(rawPassword, encryptedPassword);
    }
}
