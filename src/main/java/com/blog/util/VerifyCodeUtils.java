package com.blog.util;

import java.util.Random;

//验证码生成工具类
public class VerifyCodeUtils {
    public static String generate6DigitalCode(){
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < 6; i++){
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
