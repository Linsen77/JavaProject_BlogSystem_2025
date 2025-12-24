package com.blog.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

//邮箱发送工具类
@Component
public class EmailUtils {
    // 发件人邮箱（从配置文件读取）
    @Value("${spring.mail.username}")
    private String fromEmail;

    // Spring自带的邮件发送器
    @Resource
    private JavaMailSender mailSender;

    public void sendRegisterCode(String toEmail, String code) {
        // 构建简单邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail); // 发件人
        message.setTo(toEmail);     // 收件人
        message.setSubject("Saki_orz Blog注册验证码"); // 邮件标题
        // 邮件内容
        message.setText("验证码：" + code + "\n该验证码5分钟内有效");

        // 发送邮件
        mailSender.send(message);
    }
}
